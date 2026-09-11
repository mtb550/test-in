package org.testin.creator;

import org.testin.notifications.Done;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.creator.dialogs.CreateRunDialog;
import org.testin.creator.dialogs.CreateTestDialog;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.model.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.model.dto.dirs.TestRunsMainDirectoryDto;
import org.testin.model.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.editor.EditorUtil;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * UC-TREE-PANEL-007, UC-TREE-PANEL-009.
 * <p>
 * Declared in {@code plugin.xml} (#119), with Ctrl+M as its default: it is a
 * command a tester would look for in Find Action and might want on another key,
 * so it belongs in the keymap rather than nailed to the tree.
 */
public class CreateTreeNodeAction extends DumbAwareAction {

    /**
     * What the entry says when it does work. Taken from the bundle rather than
     * written here, because it is the same sentence plugin.xml gives the action
     * and the Keymap page shows (#11).
     */
    private static final @NotNull String CREATES = Bundle.message("action.Testin.CreateNode.description");

    // UC-TREE-PANEL-007, UC-TREE-PANEL-009
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.singleSelectedNode(e).ifPresent(dir -> new Work(p).createUnder(dir));
    }

    // UC-TREE-PANEL-007, Rule-TREE-PANEL-025
    @Override
    public void update(final @NotNull AnActionEvent e) {

        // A test project holds its two fixed containers and nothing else, so there
        // is nothing to create directly under it - which is what it answers, so
        // the capability flag is the whole question. It used to be asked twice,
        // once by name and once by capability, and the instanceof never removed
        // anything the flag would have kept.
        // And one parent to create under. With several selected there is no one
        // answer to "under which", so the entry grays rather than picking the
        // first (#192).
        final @NotNull Optional<DirectoryDto> selected = TestinData.singleSelectedNode(e);
        final boolean enabled = selected.filter(DirectoryDto::canCreateChildren).isPresent();

        // TEMPORARY - delete once Ctrl+M is understood. Only on a keystroke:
        // update() runs on every menu repaint, and a line per repaint would bury
        // the one press being asked about.
        if (ActionPlaces.KEYBOARD_SHORTCUT.equals(e.getPlace())) {
            Logger.info("[ctrl-m] CreateNode enabled=" + enabled
                    + " nodes=" + TestinData.selectedNodes(e).size()
                    + " selected=" + selected.map(dir -> dir.getType() + " '" + dir.getName() + "'").orElse("none"));
        }

        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setDescription(whyNot(selected));
    }

    /**
     * UC-TREE-PANEL-007, Rule-TREE-PANEL-096.
     * <p>
     * Why the entry is gray, in the tester's words - or what it does when it is
     * not.
     * <p>
     * It said nothing. A tester standing on a test project, a test set or a test
     * run pressed CTRL+M and got silence, because those three hold what they
     * hold and nothing is created directly under them: only the two fixed
     * containers and the two package kinds can take a new node. Correct, and
     * indistinguishable from a key that is not bound - which is the whole of
     * what the no-hidden-buttons rule is against.
     * <p>
     * The reason names the node kind rather than listing the four that work,
     * because a tester is standing on one node and wants to know about that one.
     */
    private static @NotNull String whyNot(final @NotNull Optional<DirectoryDto> selected) {
        if (selected.isEmpty()) {
            return "Select one test cases directory, test runs directory, or package to create under.";
        }

        final @NotNull DirectoryDto dir = selected.orElseThrow();
        if (dir.canCreateChildren()) return CREATES;

        return "A " + dir.getType().getMarkerKind() + " holds what it already holds - create under a "
                + DirectoryType.TSP.getDescription() + ", a " + DirectoryType.TRP.getDescription()
                + ", or one of the two directories under the test project.";
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    /**
     * Creating one node, for a project that is there.
     */
    private record Work(@NotNull Project p) {

    /**
     * UC-TREE-PANEL-007, UC-TREE-PANEL-008, UC-TREE-PANEL-009, UC-TREE-PANEL-010, Rule-TREE-PANEL-004.
     * <p>
     * Everything the action does once it knows which node it is creating under.
     */
    private void createUnder(final @NotNull DirectoryDto pDir) {
        final @NotNull BiConsumer<String, DirectoryType> onCreate = (s, dt) -> {

            if (s.isEmpty()) return;
            final @NotNull Path newDirPath = pDir.getPath().resolve(s);

            // Every node created from the tree passes through here - test set,
            // test set package, test run, test run package - so the name is
            // checked once for all four. None of the creators checked: a test set
            // created with a name already in use did not fail, it adopted the
            // existing directory and every test case in it, and rewrote its
            // marker. The tester saw "Node created" and got somebody else's set.
            if (Services.getInstance(p, ProjectIndexer.class).nodeExists(newDirPath)) {
                Services.getInstance(p, Notifier.class).softRefuse(p, Refused.ALREADY_EXISTS, s);
                return;
            }

            final @NotNull Optional<DirectoryDto> created = dt.getAction().apply(p).execute(s, pDir, newDirPath);
            Services.getInstance(p, TreePanel.class).getProjectTree().refresh();

            // Asynchronous creators (test runs) answer with nothing and run their
            // own follow-up once their dialog completes - including their own
            // confirmation, which is why this one is inside the ifPresent.
            created.ifPresent(dir -> {
                Services.getInstance(p, Notifier.class).softShow(p, Done.CREATED);

                if (dt == DirectoryType.TS)
                    Services.getInstance(p, EditorUtil.class).open(p, dir);

                dt.getCodegen().execute(p, dir);
            });

        };

        // Each node family has its own declarative dialog (issue #11).
        if (pDir instanceof TestCasesMainDirectoryDto || pDir instanceof TestSetPackageDirectoryDto) {
            new CreateTestDialog(p, onCreate).show();
            return;
        }

        if (pDir instanceof TestRunsMainDirectoryDto || pDir instanceof TestRunPackageDirectoryDto) {
            new CreateRunDialog(p, onCreate).show();
        }
    }

    }
}
