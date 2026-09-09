package org.testin.creator;

import org.testin.notifications.Done;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.creator.dialogs.CreateRunDialog;
import org.testin.creator.dialogs.CreateTestDialog;
import org.testin.explorer.TreePanel;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.DirectoryType;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.model.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.model.dto.dirs.TestRunsMainDirectoryDto;
import org.testin.model.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.editor.EditorUtil;
import org.testin.util.Shortcuts;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiConsumer;

public class CreateTreeNodeAction extends AbstractProjectTreeAction {

    public CreateTreeNodeAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "Create", "Create new node", AllIcons.General.Add);
        this.registerCustomShortcutSet(Shortcuts.CreateItem.getCustomShortcut(), tree);
    }

    // UC-TREE-PANEL-007, UC-TREE-PANEL-009
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        TreeValueUtil.singleSelectedDirectory(tree).ifPresent(this::createUnder);
    }

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
        e.getPresentation().setEnabled(TreeValueUtil.singleSelectedDirectory(tree)
                .filter(DirectoryDto::canCreateChildren)
                .isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}
