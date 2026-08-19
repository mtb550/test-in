package org.testin.git;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.*;
import org.testin.util.Shortcuts;

import java.awt.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

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

    /**
     * The button's two answers. Push is first, so it is the default and what
     * Enter does.
     */
    private static final @NotNull String PUSH = "Commit & Push";
    private static final @NotNull String COMMIT = "Commit";

    private final @NotNull List<PendingChange> rowDifferences = new ArrayList<>();
    private final @NotNull Path repoRoot;
    private final @NotNull SelectionTable changes;
    private final @NotNull TextInput message;
    private final @NotNull DialogSplitButton commit;
    private final @NotNull Consumer<Request> onCommit;

    public PendingCommitsDialog(final @NotNull Project p,
                                final @NotNull List<PendingChange> differences,
                                final @NotNull Path repoRoot,
                                final @NotNull Consumer<Request> onCommit) {
        super(p);
        this.repoRoot = repoRoot;
        this.onCommit = onCommit;

        title = "Pending Changes";

        final ComponentDialogBase<SelectionTable> table = ComponentDialogBase.table()
                .column("Change Type", 150)
                .column("Test Set", 150)
                .column("Name", 240)
                .column("Before", 180)
                .column("After", 180)
                .build();
        // Deliberately empty. Pre-filling it produced five commits called
        // "Updated test cases" in one afternoon of testing - a default that gets
        // accepted rather than read, and a history that tells a reviewer nothing.
        final ComponentDialogBase<TextInput> messageField = ComponentDialogBase.textField()
                .placeholder("what changed, in a line...")
                .build();
        // Push first, because a commit nobody pushed helps no colleague - the
        // review used to end in a commit and then offer the push in a
        // notification, which is a second decision taken away from the changes
        // it is about. Commit alone stays, one click under it.
        final ComponentDialogBase<DialogSplitButton> commitButton =
                ComponentDialogBase.splitButton(PUSH, COMMIT);

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

        // With nothing selected there is nothing to commit, and the tester can
        // deselect every row - so the button follows the selection rather than
        // letting them press it and be told afterward.
        changes.onSelectionChanged(() -> commit.setEnabled(!changes.getSelectedRows().isEmpty()));
        commit.setEnabled(!changes.getSelectedRows().isEmpty());
    }

    /**
     * One row per changed field, not per changed file: a case with three edited
     * fields is three rows, so each can be reverted on its own. A run and a
     * marker contribute rows the same way - what changed inside them, one line
     * each - though neither offers a revert.
     * <p>
     * The name column is whatever the row is about: a test case's description, a
     * run's name, the node a marker belongs to. The test set beside it is filled
     * for a test case and blank for the rest, because a run belongs to no test
     * set and saying otherwise would be a guess.
     */
    private void fillRows(final @NotNull List<PendingChange> differences) {
        for (final PendingChange diff : differences) {
            for (final FieldChange change : diff.fieldChanges()) {
                changes.addRow(
                        change.changeType().getLabel(),
                        diff.testSet(),
                        diff.name(),
                        change.oldValue(),
                        change.newValue());
                rowDifferences.add(diff);
            }
        }
    }

    /**
     * The changes the tester left selected, one entry per test case however many
     * of its rows are selected.
     */
    private @NotNull List<PendingChange> selectedDifferences() {
        final Set<PendingChange> selected = new LinkedHashSet<>();
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

        final PendingChange diff = rowDifferences.get(row);

        // A run's verdicts and a node's marker are records of work rather than
        // edits: putting one back would say a case was never run, or that a
        // project was never archived. Only a test case reverts.
        if (!diff.isRevertible()) {
            Services.getInstance(p, Notifier.class)
                    .softShow(p, "Only a test case change can be reverted");
            return;
        }

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
        final List<PendingChange> selected = selectedDifferences();
        if (selected.isEmpty()) return;

        // Said here, next to the empty field, rather than as a balloon after the
        // dialog closed and took the changes off the screen with it.
        if (message.getText().isBlank()) {
            message.showEmptyWarning();
            return;
        }

        onCommit.accept(new Request(selected, message.getText().trim(), PUSH.equals(commit.getChosen())));
        closeOk();
    }

    /**
     * What the tester asked for: these changes, under this message, and whether
     * it goes to the remote as well as into the local history.
     */
    public record Request(@NotNull List<PendingChange> changes, @NotNull String message, boolean push) {
    }
}
