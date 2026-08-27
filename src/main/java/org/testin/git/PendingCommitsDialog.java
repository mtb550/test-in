package org.testin.git;

import org.testin.notifications.Done;
import org.testin.model.DirectoryType;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.*;

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

    /**
     * The button's two answers. Push is first, so it is the default and what
     * Enter does.
     */
    private static final @NotNull String PUSH = "Commit & Push";
    private static final @NotNull String COMMIT = "Commit";

    /**
     * One entry per table row, holding the change the row was drawn from.
     * <p>
     * The field change is kept, not just the pending change it belongs to. It
     * used to be thrown away here and reconstructed in revert by parsing the
     * change-kind caption back out of the table cell - so rewording any caption
     * stopped revert working on that row, with no error, no balloon and no
     * compile failure, on the one destructive button in this dialog.
     */
    private final @NotNull List<Row> rowDifferences = new ArrayList<>();
    private final @NotNull Path repoRoot;
    private final @NotNull SelectionTable changes;
    private final @NotNull ChoiceInput branch;
    private final @NotNull TextInput message;
    private final @NotNull DialogSplitButton commit;
    private final @NotNull Consumer<Request> onCommit;

    /**
     * @param branches      the branches to offer, read by the caller. Every Git
     *                      command in this plugin goes through git4idea's
     *                      authentication setup, which asserts it is not on the
     *                      EDT - and a dialog constructor is on the EDT. So the
     *                      background pass that collected the changes collects
     *                      these too
     * @param currentBranch the branch the repository is on, empty when Git could
     *                      not say - a repository with no commit yet has a
     *                      branch name and no branch
     */
    public PendingCommitsDialog(final @NotNull Project p, final @NotNull List<PendingChange> differences, final @NotNull Path repoRoot, final @NotNull List<String> branches, final @NotNull String currentBranch, final @NotNull Consumer<Request> onCommit) {
        super(p);
        this.repoRoot = repoRoot;
        this.onCommit = onCommit;

        title = "Pending Changes";

        final @NotNull ComponentDialogBase<SelectionTable> table = ComponentDialogBase.table()
                .column("Change Type", 150)
                .column(DirectoryType.TS.getDisplayedName(), 150)
                .column("Name", 240)
                .column("Before", 180)
                .column("After", 180)
                .build();
        // The branch is part of the review, not a thing to remember to do
        // first. It offers the branches this machine has, on the one that is
        // checked out, and takes a name that is not on the list as a new branch
        // to start - which is how a tester keeps a cycle's results off main
        // without leaving the dialog.
        final @NotNull ComponentDialogBase<ChoiceInput> branchRow =
                ComponentDialogBase.choice("Branch", offered(branches, currentBranch), currentBranch);

        // Deliberately empty. Pre-filling it produced five commits called
        // "Updated test cases" in one afternoon of testing - a default that gets
        // accepted rather than read, and a history that tells a reviewer nothing.
        final @NotNull ComponentDialogBase<TextInput> messageField = ComponentDialogBase.textField()
                .placeholder("what changed, in a line...")
                .build();
        // Push first, because a commit nobody pushed helps no colleague - the
        // review used to end in a commit and then offer the push in a
        // notification, which is a second decision taken away from the changes
        // it is about. Commit alone stays, one click under it.
        final @NotNull ComponentDialogBase<DialogSplitButton> commitButton =
                ComponentDialogBase.splitButton(PUSH, COMMIT);

        components = List.of(table, branchRow, messageField, commitButton);
        changes = table.getComponent();
        branch = branchRow.getComponent();
        message = messageField.getComponent();
        commit = commitButton.getComponent();

        shortcuts = List.of(
                StatusBarShortcut.hint("Right click", "Revert a change"),
                StatusBarShortcut.cancel(this::closeCancel));

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
     * The branches to offer, with the current one always among them: it is the
     * selected value, and a list that did not contain its own selection would
     * read as a branch about to be created. A repository with no commit yet has
     * exactly that shape - Git names the branch and lists none.
     */
    private static @NotNull List<String> offered(final @NotNull List<String> branches, final @NotNull String current) {
        if (current.isEmpty() || branches.contains(current)) return branches;

        final @NotNull List<String> withCurrent = new ArrayList<>(branches);
        withCurrent.addFirst(current);

        return withCurrent;
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
                rowDifferences.add(new Row(diff, change));
            }
        }
    }

    /**
     * The changes the tester left selected, one entry per test case however many
     * of its rows are selected.
     */
    private @NotNull List<PendingChange> selectedDifferences() {
        final @NotNull Set<PendingChange> selected = new LinkedHashSet<>();
        for (final int row : changes.getSelectedRows()) {
            if (row < rowDifferences.size()) selected.add(rowDifferences.get(row).diff());
        }
        return List.copyOf(selected);
    }

    /**
     * One table row: the change to a file, and the one field of it this row is
     * about. A test case with three edited fields contributes three rows, so the
     * field cannot be read back off the pending change.
     */
    private record Row(@NotNull PendingChange diff, @NotNull FieldChange change) {
    }

    /**
     * Puts one field back to what was committed, and takes its row away.
     */
    private void revertRow(final @NotNull Project p, final int row) {
        if (row >= rowDifferences.size()) return;

        final @NotNull PendingChange diff = rowDifferences.get(row).diff();
        final @NotNull ChangeType changeType = rowDifferences.get(row).change().changeType();

        // A run's verdicts and a node's marker are records of work rather than
        // edits: putting one back would say a case was never run, or that a
        // project was never archived. Only a test case reverts.
        if (!diff.isRevertible()) {
            Services.getInstance(p, Notifier.class)
                    .softRefuse(p, "Only a test case change can be reverted");
            return;
        }

        try {
            final @NotNull Optional<Path> found = Optional.ofNullable(repoRoot.resolve(diff.relativeFilePath()).getParent());
            if (found.isEmpty()) return;

            final @NotNull Path testSetPath = found.orElseThrow();
            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            final @NotNull UUID testCaseId = UUID.fromString(diff.testCaseId());

            switch (diff.type()) {
                case ADDED -> indexer.removeTestCase(testSetPath, testCaseId);
                case DELETED -> indexer.putTestCase(testSetPath, diff.committedState());
                case MODIFIED -> {
                    // Both refusals say so now. They shared one silent return,
                    // so a tester who pressed Revert on a field that cannot be
                    // put back, or on a case that had since been deleted, saw
                    // the row sit there with nothing explaining why.
                    if (!changeType.isRevertable()) {
                        Services.getInstance(p, Notifier.class)
                                .softRefuse(p, "A change to " + changeType.getLabel() + " cannot be reverted");
                        return;
                    }

                    final @NotNull Optional<TestCaseDto> current = indexer.findTestCase(testCaseId);
                    if (current.isEmpty()) {
                        Services.getInstance(p, Notifier.class)
                                .softRefuse(p, "That test case is no longer in the project");
                        return;
                    }

                    changeType.getRevertAction().apply(current.orElseThrow(), diff.committedState());
                    indexer.putTestCase(testSetPath, current.orElseThrow());
                }
            }

            removeRow(row);
            Services.getInstance(p, Notifier.class).softShow(p, Done.REVERTED);

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
        final @NotNull List<PendingChange> selected = selectedDifferences();
        if (selected.isEmpty()) return;

        // Said here, next to the empty field, rather than as a balloon after the
        // dialog closed and took the changes off the screen with it.
        if (message.getText().isBlank()) {
            message.showEmptyWarning();
            return;
        }

        onCommit.accept(new Request(selected, message.getText().trim(),
                PUSH.equals(commit.getChosen()), branch.getValue(), branch.isNew()));
        closeOk();
    }

    /**
     * What the tester asked for: these changes, under this message, onto this
     * branch, and whether it goes to the remote as well as into the local
     * history.
     *
     * @param branch    where the commit goes. The branch that is checked out
     *                  unless the tester picked or typed another
     * @param newBranch true when the name is not one of the branches offered, so
     *                  the commit starts it rather than switching to it. Decided
     *                  here, where the list that was offered is known, rather
     *                  than by asking Git again later and racing whoever else
     *                  touched the repository in between
     */
    public record Request(@NotNull List<PendingChange> changes, @NotNull String message, boolean push, @NotNull String branch, boolean newBranch) {
    }
}
