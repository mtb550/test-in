package org.testin.git;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.DialogButton;
import org.testin.ui.framework.SelectionTable;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;
import org.testin.util.Shortcuts;

import java.awt.Dimension;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Review what changed and commit it: the changed fields as rows, a message, and
 * a Commit button.
 * <p>
 * On {@code ui.framework} rather than hand-built (#69), which is what gives it a
 * status bar — the keys it binds are now the keys it shows, instead of binding
 * Enter and Escape and advertising neither (#66).
 * <p>
 * The message used to be a second platform prompt raised after this dialog
 * closed, so the tester confirmed a selection and only then found out a message
 * was wanted, with the changes no longer in front of them. It is a field here,
 * beside the button that uses it.
 */
public final class PendingCommitsDialog extends AbstractFrameworkDialog<SelectionTable> {

    private static final int COLUMN_CHANGE_TYPE = 0;

    private final @NotNull List<TestCaseDiff> rowDifferences = new ArrayList<>();
    private final @NotNull Path repoRoot;
    private final @NotNull SelectionTable changes;
    private final @NotNull TextInput message;
    private final @NotNull DialogButton commit;
    private final @NotNull BiConsumer<List<TestCaseDiff>, String> onCommit;

    public PendingCommitsDialog(final @NotNull Project p,
                                final @NotNull List<TestCaseDiff> differences,
                                final @NotNull Path repoRoot,
                                final @NotNull BiConsumer<List<TestCaseDiff>, String> onCommit) {
        super(p);
        this.repoRoot = repoRoot;
        this.onCommit = onCommit;

        title = "Pending Test Case Changes";

        final ComponentDialogBase<SelectionTable> table = ComponentDialogBase.table()
                .column("Change Type", 140)
                .column("Test Case", 260)
                .column("Before", 200)
                .column("After", 200)
                .build();
        // Deliberately empty. Pre-filling it produced five commits called
        // "Updated test cases" in one afternoon of testing - a default that gets
        // accepted rather than read, and a history that tells a reviewer nothing.
        final ComponentDialogBase<TextInput> messageField = ComponentDialogBase.textField()
                .placeholder("what changed, in a line...")
                .build();
        final ComponentDialogBase<DialogButton> commitButton = ComponentDialogBase.button("Commit");

        components = List.of(table, messageField, commitButton);
        changes = table.getComponent();
        message = messageField.getComponent();
        commit = commitButton.getComponent();

        shortcuts = List.of(
                StatusBarShortcut.hint("Right click", "Revert a change"),
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel));

        preferredSize = new Dimension(JBUI.scale(1000), JBUI.scale(500));

        fillRows(differences);
        changes.selectAll();
        changes.onRowAction("Revert this change", row -> revertRow(p, row));

        // Nothing selected is nothing to commit, and the tester can deselect
        // every row - so the button follows the selection rather than letting
        // them press it and be told afterwards.
        changes.onSelectionChanged(() -> commit.setEnabled(!changes.getSelectedRows().isEmpty()));
        commit.setEnabled(!changes.getSelectedRows().isEmpty());
    }

    /**
     * One row per changed field, not per test case: a case with three edited
     * fields is three rows, so each can be reverted on its own.
     */
    private void fillRows(final @NotNull List<TestCaseDiff> differences) {
        for (final TestCaseDiff diff : differences) {
            for (final FieldChange change : diff.fieldChanges()) {
                changes.addRow(
                        change.changeType().getLabel(),
                        describe(diff, change),
                        change.oldValue(),
                        change.newValue());
                rowDifferences.add(diff);
            }
        }
    }

    /**
     * The test case's description, taken from whichever side of the change still
     * has one — a deleted case only exists on the old side.
     */
    private @NotNull String describe(final @NotNull TestCaseDiff diff, final @NotNull FieldChange change) {
        final TestCaseDto state = diff.type() == DiffType.DELETED ? diff.oldState() : diff.newState();
        if (state != null) return state.getDescription();

        final String fallback = diff.type() == DiffType.DELETED ? change.oldValue() : change.newValue();
        return fallback == null ? "" : fallback;
    }

    /**
     * The changes the tester left selected, one entry per test case however many
     * of its rows are selected.
     */
    private @NotNull List<TestCaseDiff> selectedDifferences() {
        final Set<TestCaseDiff> selected = new LinkedHashSet<>();
        for (final int row : changes.getSelectedRows()) {
            if (row < rowDifferences.size()) selected.add(rowDifferences.get(row));
        }
        return List.copyOf(selected);
    }

    /**
     * Puts one field back to what was committed, and takes its row away.
     * <p>
     * The row's own change-type label says which field it reverts: one test case
     * can contribute several rows, so the field cannot be read off the diff.
     */
    private void revertRow(final @NotNull Project p, final int row) {
        if (row >= rowDifferences.size()) return;

        final TestCaseDiff diff = rowDifferences.get(row);
        final ChangeType changeType = ChangeType.fromLabel(changes.getValueAt(row, COLUMN_CHANGE_TYPE));

        try {
            final Path testSetPath = repoRoot.resolve(diff.relativeFilePath()).getParent();
            if (testSetPath == null) return;

            final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            final UUID testCaseId = UUID.fromString(diff.testCaseId());

            switch (diff.type()) {
                case ADDED -> indexer.removeTestCase(testSetPath, testCaseId);
                case DELETED -> {
                    final TestCaseDto oldState = diff.oldState();
                    if (oldState == null) return;
                    indexer.putTestCase(testSetPath, oldState);
                }
                case MODIFIED -> {
                    final TestCaseDto current = indexer.getTestCaseById(testCaseId);
                    final TestCaseDto oldState = diff.oldState();
                    final RevertAction revert = changeType == null ? null : changeType.getRevertAction();
                    if (current == null || oldState == null || revert == null) return;

                    revert.apply(current, oldState);
                    indexer.putTestCase(testSetPath, current);
                }
            }

            removeRow(row);
            Services.getInstance(p, Notifier.class).softShow(p, "Reverted");

        } catch (final Exception ex) {
            Services.getInstance(p, Notifier.class).error(p, "Revert Failed", "Could not revert change: " + ex.getMessage());
        }
    }

    private void removeRow(final int row) {
        changes.removeRow(row);
        rowDifferences.remove(row);
        commit.setEnabled(!changes.getSelectedRows().isEmpty());
    }

    @Override
    protected void submit() {
        final List<TestCaseDiff> selected = selectedDifferences();
        if (selected.isEmpty()) return;

        // Said here, next to the empty field, rather than as a balloon after the
        // dialog closed and took the changes off the screen with it.
        if (message.getText().isBlank()) {
            message.showEmptyWarning();
            return;
        }

        onCommit.accept(selected, message.getText().trim());
        closeOk();
    }
}
