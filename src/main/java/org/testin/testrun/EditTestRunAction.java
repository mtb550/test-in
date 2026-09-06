package org.testin.testrun;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.explorer.TreePanel;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.TestRunConfiguration;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.rename.NodeRename;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.ui.framework.SelectionTree;
import org.testin.undo.UndoScope;
import org.testin.undo.UndoService;
import org.testin.util.BackgroundWork;
import org.testin.util.EditorUtil;
import org.testin.util.Mapper;

import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Changing a run that already exists: which test cases it covers, what it is
 * called, and how it was configured (#96).
 * <p>
 * The cases a run covered used to be chosen once, in the create dialog, and
 * never again. That is the wrong number of chances - a tester starts executing a
 * cycle, remembers a case the set was missing, adds it to the test set, and then
 * has nowhere to put it: the run they are part way through cannot see it, and
 * covering it meant creating a second run and abandoning the verdicts already
 * recorded in the first.
 * <p>
 * This opens the same form creating a run opens, holding what the run holds now,
 * and writes the difference back. The tree is rebuilt from the index every time,
 * which is the point: a case added after the run was created appears here.
 * <p>
 * <b>Only while the run is open.</b> A completed or closed run has been signed
 * off and reported on, and what a report says must not move underneath it - so
 * the action is disabled rather than asking (#84).
 * <p>
 * <b>Unticking an executed case discards its result.</b> The verdict, the actual
 * result, the bug severity and priority, the duration, who ran it and when, and
 * the stack trace all go, with no confirmation - decided on 2026-09-03, against
 * a recommendation to refuse the untick instead. The undo below is what that
 * decision leans on: the whole edit is one entry, so a mis-click is one Ctrl+Z
 * rather than a loss.
 * <p>
 * <b>A case the dialog cannot show is kept.</b> That decision is about unticking,
 * and a case deleted from its test set has no row to untick - it is not in the
 * tree this dialog builds from the index. Reading its absence as an untick threw
 * away what the run recorded about it, on a Save that changed nothing (#190).
 */
public class EditTestRunAction extends AbstractProjectTreeAction {

    private final @NotNull TreePanel tp;

    public EditTestRunAction(final @NotNull Project p, final @NotNull TreePanel tp, final @NotNull SimpleTree tree) {
        super(p, tree, "Edit Run", "Change which test cases this run covers, its name and its configuration", AllIcons.Actions.Edit);
        this.tp = tp;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        Optional.ofNullable(tree.getSelectionPath()).ifPresent(this::editAt);
    }

    /**
     * The run and the folder it sits in, read off the same path - the parent is
     * taken from the tree rather than from the node, which carries it as a field
     * that may not be set.
     */
    private void editAt(final @NotNull TreePath path) {
        selectedRun(TreeValueUtil.directoryAt(path))
                .ifPresent(run -> TreeValueUtil.directoryAt(path.getParentPath())
                        .ifPresent(parent -> edit(run, parent)));
    }

    private void edit(final @NotNull TestRunDirectoryDto run, final @NotNull DirectoryDto parent) {
        final @NotNull TestRunDto current = Services.getInstance(p, ProjectIndexer.class).getTestRunByPath(run.getPath());
        final @NotNull Set<UUID> covered = current.getResults().stream().map(TestRunItems::getId).collect(Collectors.toSet());

        Services.getInstance(p, BoundTestProject.class).get().ifPresentOrElse(
                tp -> new RunForm(p).open(tp.getTestCasesDirectory(), run.getName(), covered, current.getConfiguration(), saves(run, parent, current)),
                () -> Logger.warn("Edit test run: no test project is bound to " + p.getName()));
    }

    private @NotNull RunFormAction saves(final @NotNull TestRunDirectoryDto run, final @NotNull DirectoryDto parent, final @NotNull TestRunDto current) {
        return new RunFormAction("Edit Test Run", "Save", (form, selection) -> save(run, parent, current, form, selection));
    }

    /**
     * UC-TREE-PANEL-022, Rule-TREE-PANEL-061 and Rule-TREE-PANEL-089.
     * <p>
     * Writes the change, or refuses and says why - and answers which, because the
     * dialog stays open on a refusal with everything the tester typed still in it.
     */
    private boolean save(final @NotNull TestRunDirectoryDto run, final @NotNull DirectoryDto parent, final @NotNull TestRunDto current, final @NotNull RunConfigurationForm form, final @NotNull SelectionTree selection) {
        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        final @NotNull String name = form.getRunName();
        if (name.isEmpty()) {
            notifier.softRefuse(p, "A test run needs a name");
            return false;
        }

        // The dialog is not modal - the tree stays live while it is open, so the
        // run may have been removed, or signed off from its own editor, since.
        if (!indexer.nodeExists(run.getPath())) {
            notifier.softRefuse(p, "'" + run.getName() + "' no longer exists - nothing saved");
            return false;
        }

        if (!run.isStillOpen()) {
            notifier.softRefuse(p, "'" + run.getName() + "' was " + run.getMarker().getStatusLabel() + " while this was open - nothing saved");
            return false;
        }

        // Its own name is not a collision, so a tester who edits the cases without
        // touching the name is not refused for keeping it.
        final @NotNull String oldName = run.getName();
        if (!name.equals(oldName) && indexer.nodeExists(parent.getPath().resolve(name))) {
            notifier.softShowExists(p, name);
            return false;
        }

        // The dialog's tree is built from the test cases that still exist, so a
        // case deleted from its test set has no row in it. It could not be ticked,
        // which means it cannot have been unticked either, and the dialog's answer
        // says nothing about it. Taken as an answer it used to delete the verdict,
        // the actual result, the stacktrace, the bug severity, the bug priority
        // and the duration the run had recorded - on a Save that changed nothing,
        // with no row to see it go and no copy anywhere (#190).
        //
        // A run outlives the test cases it was made from, and keeps what it
        // recorded about them (#71). Unticking a case the tester could see still
        // discards its result, which is what unticking is for.
        final @NotNull Set<UUID> wanted = new LinkedHashSet<>(RunForm.checkedCases(selection));
        current.getResults().stream()
                .map(TestRunItems::getId)
                .filter(id -> indexer.findTestCase(id).isEmpty())
                .forEach(wanted::add);

        final @NotNull TestRunDto after = current.coverOnly(wanted)
                .setConfiguration(TestRunConfiguration.answered(form.configuration()));

        // Copied rather than held: the run in the indexer's cache shares its
        // result objects with this one, and a verdict recorded between now and
        // the undo would otherwise change what the undo puts back.
        final @NotNull TestRunDto before = copyOf(current);

        applyEdit(run, name, after, () -> Services.getInstance(p, Notifier.class).softShow(p, Done.UPDATED));

        // One entry for the whole edit - the cases, the name and the
        // configuration together - because the tester made one gesture. The dto
        // reference stays valid across renames, so undo and redo are the same
        // routine with the two sides swapped.
        Services.getInstance(p, UndoService.class).push(UndoScope.TREE, new UndoService.Operation(
                "Edit '" + oldName + "'",
                () -> applyEdit(run, oldName, before, () -> {
                }),
                () -> applyEdit(run, name, after, () -> {
                })));

        return true;
    }

    /**
     * The rename first, when there is one, and the run written into wherever the
     * folder ended up.
     * <p>
     * Renaming through {@link NodeRename} rather than here, so a run renamed from
     * this dialog behaves exactly as one renamed from the tree does - the editor
     * closes, the codegen is told, and the tree refreshes when the indexer has
     * finished rather than before.
     */
    private void applyEdit(final @NotNull TestRunDirectoryDto run, final @NotNull String toName, final @NotNull TestRunDto content, final @NotNull Runnable onDone) {
        final @NotNull Path from = run.getPath();

        if (toName.equals(run.getName())) {
            write(from, content, onDone);
            return;
        }

        NodeRename.apply(p, tp, run, toName, () -> write(from.getParent().resolve(toName), content, onDone));
    }

    private void write(final @NotNull Path runPath, final @NotNull TestRunDto content, final @NotNull Runnable onDone) {
        BackgroundWork.run(p, "Updating test run " + runPath.getFileName(), "Test Run Not Updated", indicator -> {
            Services.getInstance(p, ProjectIndexer.class).putTestRun(runPath, content);

            // File access is the indexer's alone (see CLAUDE.md).
            Services.getInstance(p, ProjectIndexer.class).refreshDirectory(runPath);

            ApplicationManager.getApplication().invokeLater(() -> {
                tp.getProjectTree().refresh();

                // Nothing to reload when the name changed - the rename closed the
                // editor before the node moved.
                Services.getInstance(p, EditorUtil.class).reloadOpen(p, runPath);

                onDone.run();
            });
        });
    }

    /**
     * A run detached from the one the indexer is holding, through the same mapper
     * that reads it off disk.
     */
    private @NotNull TestRunDto copyOf(final @NotNull TestRunDto run) {
        final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);
        return mapper.readValue(mapper.writeValueAsString(run), TestRunDto.class);
    }

    private static @NotNull Optional<TestRunDirectoryDto> selectedRun(final @NotNull Optional<DirectoryDto> dir) {
        return dir.filter(TestRunDirectoryDto.class::isInstance)
                .map(TestRunDirectoryDto.class::cast)
                .filter(TestRunDirectoryDto::isStillOpen);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(selectedRun(TreeValueUtil.singleSelectedDirectory(tree)).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads the tree's selection, which is Swing state.
        return ActionUpdateThread.EDT;
    }
}
