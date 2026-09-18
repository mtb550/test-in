/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.testrun;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.actions.TestinData;
import org.testin.explorer.TreePanel;
import org.testin.explorer.tree.TreeValues;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.TestRunConfiguration;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.rename.NodeRename;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.ui.framework.SelectionTree;
import org.testin.undo.UndoScope;
import org.testin.undo.UndoHistories;
import org.testin.services.BackgroundWork;
import org.testin.editor.TestinEditors;
import org.testin.util.Bundle;
import org.testin.util.Mapper;

import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Consumer;
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
 * <p>
 * Declared in {@code plugin.xml} (#119), so Find Action offers it and a tester
 * can bind a key to it - it has never had one. No constructor and no fields: the
 * platform builds one instance for the whole IDE, so the run comes from the
 * keystroke, and the edit itself is in {@link Work}.
 */
public class EditTestRunAction extends DumbAwareAction {

    // UC-TREE-PANEL-022
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.tree(e)
                .map(SimpleTree::getSelectionPath)
                .ifPresent(path -> new Work(p).editAt(path));
    }

    /**
     * UC-TREE-PANEL-022, Rule-TREE-PANEL-073.
     * <p>
     * On one open run and nothing else - including outside the Testin tree,
     * where nothing is selected at all, which is what keeps a key bound to this
     * gray in a Java file (#119).
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(selectedRun(TestinData.singleSelectedNode(e)).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads the tree's selection, which is Swing state.
        return ActionUpdateThread.EDT;
    }

    private static @NotNull Optional<TestRunDirectoryDto> selectedRun(final @NotNull Optional<DirectoryDto> dir) {
        return dir.filter(TestRunDirectoryDto.class::isInstance)
                .map(TestRunDirectoryDto.class::cast)
                .filter(TestRunDirectoryDto::isStillOpen);
    }

    /**
     * Editing one run, for a project that is there.
     */
    private record Work(@NotNull Project p) {

        /**
         * The run and the folder it sits in, read off the same path - the parent is
         * taken from the tree rather than from the node, which carries it as a field
         * that may not be set.
         */
        private void editAt(final @NotNull TreePath path) {
            selectedRun(TreeValues.directoryAt(path))
                    .ifPresent(run -> TreeValues.directoryAt(path.getParentPath())
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
            return new RunFormAction(Bundle.message("run.edit.title"), StatusBarShortcut.SAVE, (form, selection) -> save(run, parent, current, form, selection));
        }

        /**
         * UC-TREE-PANEL-022, Rule-TREE-PANEL-074, Rule-TREE-PANEL-076.
         * <p>
         * Writes the change, or refuses and says why - and answers which, because the
         * dialog stays open on a refusal with everything the tester typed still in it.
         */
        private boolean save(final @NotNull TestRunDirectoryDto run, final @NotNull DirectoryDto parent, final @NotNull TestRunDto current, final @NotNull RunConfigurationForm form, final @NotNull SelectionTree selection) {
            final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);
            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

            final @NotNull String name = form.getRunName();
            if (name.isEmpty()) {
                notifier.softRefuse(p, Bundle.message("run.needs.a.name"));
                return false;
            }

            // The dialog is not modal - the tree stays live while it is open, so the
            // run may have been removed, or signed off from its own editor, since.
            if (!indexer.nodeExists(run.getPath())) {
                notifier.softRefuse(p, Bundle.message("run.gone", run.getName()));
                return false;
            }

            if (!run.isStillOpen()) {
                notifier.softRefuse(p, Bundle.message("run.status.changed", run.getName(), run.getMarker().getStatusLabel()));
                return false;
            }

            // Its own name is not a collision, so a tester who edits the cases without
            // touching the name is not refused for keeping it.
            final @NotNull String oldName = run.getName();
            if (!name.equals(oldName) && indexer.nodeExists(parent.getPath().resolve(name))) {
                notifier.softRefuse(p, Refused.ALREADY_EXISTS, name);
                return false;
            }

            final @NotNull Set<UUID> checked = RunForm.checkedCases(selection);
            final @NotNull Set<UUID> offered = RunForm.offeredCases(selection);
            final @NotNull Map<TestRunConfiguration, String> configuration = TestRunConfiguration.answered(form.configuration());

            final @NotNull TestRunDto after = current.coverOnly(wanted(current, checked, offered::contains))
                    .setConfiguration(configuration);

            // Copied rather than held: the run in the indexer's cache shares its
            // result objects with this one, and a verdict recorded between now and
            // the undo would otherwise change what the undo puts back.
            final @NotNull TestRunDto before = copyOf(current);

            // What this edit changed, and so the whole of what undoing it puts
            // back: which cases the run covered, and how it was configured.
            final @NotNull Set<UUID> coveredBefore = idsOf(before);
            final @NotNull Map<UUID, TestRunItems> recordedBefore = byId(before);

            final @NotNull Set<UUID> coveredAfter = idsOf(after);
            final @NotNull Map<UUID, TestRunItems> recordedAfter = byId(after);

            final @NotNull Map<TestRunConfiguration, String> configurationBefore = Map.copyOf(before.getConfiguration());

            // Applied to the run the index holds when the write lands, not to the
            // one this dialog read when it opened. The dialog is not modal, so a
            // sync can bring in a newer run while it is open, and writing the run it
            // opened on replaced the verdicts that arrived. Through changeRun, which
            // a sync holds, like every other change to a run (#312, A10).
            applyEdit(run, name, runPath -> indexer.changeRun(runPath, held -> held
                    .setResults(held.coverOnly(wanted(held, checked, offered::contains)).getResults())
                    .setConfiguration(configuration)), () -> Services.getInstance(p, Notifier.class).softShow(p, Done.UPDATED));

            // One entry for the whole edit - the cases, the name and the
            // configuration together - because the tester made one gesture. The dto
            // reference stays valid across renames, so undo and redo are the same
            // routine with the two sides swapped.
            Services.getInstance(p, UndoHistories.class).push(UndoScope.TREE, new UndoHistories.Operation(
                    Bundle.message("run.undo.edit", oldName),
                    () -> putCoverageBack(run, oldName, coveredBefore, configurationBefore, recordedBefore),
                    () -> putCoverageBack(run, name, coveredAfter, configuration, recordedAfter)));

            return true;
        }

        /**
         * UC-TREE-PANEL-022, Rule-TREE-PANEL-076.
         * <p>
         * The cases a run covers after the edit: the ones ticked, and every one it
         * already recorded that the tester could not have ticked.
         * <p>
         * The dialog's tree is built from the test cases that still exist, so a
         * case deleted from its test set has no row in it. It could not be ticked,
         * which means it cannot have been unticked either, and the dialog's answer
         * says nothing about it. Taken as an answer it used to delete the verdict,
         * the actual result, the stacktrace, the bug severity, the bug priority
         * and the duration the run had recorded - on a Save that changed nothing,
         * with no row to see it go and no copy anywhere (#190).
         * <p>
         * A run outlives the test cases it was made from, and keeps what it
         * recorded about them (#71). Unticking a case the tester could see still
         * discards its result, which is what unticking is for.
         * <p>
         * <b>Asked of the tree, not of the index.</b> Only a deleted case used to
         * be kept, by asking whether the index still knew it - but the tree also
         * leaves out every case in a test set deprecated, or under a package
         * archived, since the run was made, and those the index does know. So
         * opening Edit Test Run to add one case deleted a retired suite's
         * verdicts, actual results, stacktraces, bug severities and priorities
         * with no row seen to go (#66, finding 167). What the tree offered is the
         * one list that holds exactly the cases a tester could have unticked.
         */
        private @NotNull Set<UUID> wanted(final @NotNull TestRunDto from, final @NotNull Set<UUID> checked, final @NotNull Predicate<UUID> couldBeTicked) {
            final @NotNull Set<UUID> wanted = new LinkedHashSet<>(checked);
            from.getResults().stream()
                    .map(TestRunItems::getId)
                    .filter(couldBeTicked.negate())
                    .forEach(wanted::add);
            return wanted;
        }

        /**
         * Whether the index still knows this case. The undo has no dialog to ask
         * what it offered: it puts back the coverage from before the edit, and a
         * case deleted since then is the one it must not drop.
         */
        private boolean isIndexed(final @NotNull UUID id) {
            return Services.getInstance(p, ProjectIndexer.class).findTestCase(id).isPresent();
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
        private void applyEdit(final @NotNull TestRunDirectoryDto run, final @NotNull String toName, final @NotNull Consumer<Path> writeTo, final @NotNull Runnable onDone) {
            final @NotNull Path from = run.getPath();

            if (toName.equals(run.getName())) {
                write(from, writeTo, onDone);
                return;
            }

            NodeRename.apply(p, Services.getInstance(p, TreePanel.class), run, toName, () -> write(from.getParent().resolve(toName), writeTo, onDone));
        }

        private void write(final @NotNull Path runPath, final @NotNull Consumer<Path> writeTo, final @NotNull Runnable onDone) {
            // The change itself on the EDT, which is where every other change to a
            // run is made and what changeRun is written for: it mutates the run the
            // index holds and takes the snapshot the write queue then writes, both
            // cheap, and the queue is what keeps the disk off this thread. Made
            // from a pooled thread it could be mutating the same run and the same
            // result list as a verdict being recorded under the tester's hand, with
            // nothing between them (#312, N14).
            //
            // Both callers are already here: the dialog saves on the EDT, and the
            // rename hands back inside the VFS write action, which is on it too.
            writeTo.accept(runPath);

            BackgroundWork.run(p, Bundle.message("run.task.updating", runPath.getFileName()), Bundle.message("run.update.failed.title"), indicator -> {
                // File access is the indexer's alone (see CLAUDE.md).
                Services.getInstance(p, ProjectIndexer.class).refreshDirectory(runPath);

                ApplicationManager.getApplication().invokeLater(() -> {
                    Services.getInstance(p, TreePanel.class).getProjectTree().refresh();

                    // Nothing to reload when the name changed - the rename closed the
                    // editor before the node moved.
                    Services.getInstance(p, TestinEditors.class).reloadOpen(p, runPath);

                    onDone.run();
                });
            });
        }

        /**
         * UC-TREE-PANEL-021, Rule-TREE-PANEL-009.
         * <p>
         * Puts back what this edit changed - which cases the run covers, its
         * name and its configuration - and nothing else.
         * <p>
         * It used to write the whole run back as it stood at Save. So a tester
         * who saved an edit, judged ten cases, and then pressed Ctrl+Z in the
         * tree saw <i>Undone</i> and lost all ten: every result recorded since
         * was Pending and empty again, and a run completed in between stayed
         * Completed over them (#312, A9).
         * <p>
         * A case the undo brings back gets the result it had when the edit
         * dropped it, rather than the Pending a case nobody has seen would get:
         * unticking a case discards what it recorded, so putting the tick back
         * has to put that back with it. A case covered before and after keeps
         * whatever it holds now, which is the verdict recorded since.
         * <p>
         * Through {@code changeRun} like the edit itself, so an undo during a
         * sync waits for it and lands on the run that arrived. And refused on a
         * run that has been signed off since, which is the same question Save
         * asks: what a report says must not move underneath it.
         */
        private void putCoverageBack(final @NotNull TestRunDirectoryDto run, final @NotNull String toName, final @NotNull Set<UUID> covered, final @NotNull Map<TestRunConfiguration, String> configuration, final @NotNull Map<UUID, TestRunItems> recorded) {
            if (!run.isStillOpen()) {
                Services.getInstance(p, Notifier.class).softRefuse(p,
                        Bundle.message("run.status.changed", run.getName(), run.getMarker().getStatusLabel()));
                return;
            }

            applyEdit(run, toName, runPath -> Services.getInstance(p, ProjectIndexer.class).changeRun(runPath, held -> {
                final @NotNull Set<UUID> holdsNow = idsOf(held);

                final @NotNull List<TestRunItems> items = held.coverOnly(wanted(held, covered, this::isIndexed)).getResults().stream()
                        .map(item -> holdsNow.contains(item.getId()) ? item : recorded.getOrDefault(item.getId(), item))
                        .collect(Collectors.toCollection(ArrayList::new));

                held.setResults(items).setConfiguration(configuration);
            }), () -> {
            });
        }

        /**
         * The cases a run covers, in the order it holds them.
         */
        private static @NotNull Set<UUID> idsOf(final @NotNull TestRunDto run) {
            return run.getResults().stream().map(TestRunItems::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        }

        /**
         * What the run recorded for each case it covers, so an undo that brings
         * a case back brings its verdict with it.
         */
        private static @NotNull Map<UUID, TestRunItems> byId(final @NotNull TestRunDto run) {
            return run.getResults().stream().collect(Collectors.toMap(TestRunItems::getId, item -> item, (first, second) -> first));
        }

        /**
         * A run detached from the one the indexer is holding, through the same mapper
         * that reads it off disk.
         */
        private @NotNull TestRunDto copyOf(final @NotNull TestRunDto run) {
            final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);
            return mapper.readValue(mapper.writeValueAsString(run), TestRunDto.class);
        }
    }
}
