package org.testin.creator;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.enums.DirectoryType;
import org.testin.model.dto.dirs.*;
import org.testin.creator.dialogs.CreateRunDialog;
import org.testin.creator.dialogs.CreateTestDialog;
import org.testin.notifications.Notifier;
import org.testin.explorer.ProjectPanel;
import org.testin.services.Services;
import org.testin.util.EditorUtil;
import org.testin.util.Shortcuts;
import org.testin.util.Tools;

import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public class CreateTreeNodeAction extends AbstractProjectTreeAction {
    private final @NotNull Tools tools;

    public CreateTreeNodeAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "Create", "Create new node", AllIcons.General.Add);
        this.tools = Services.getInstance(p, Tools.class);
        this.registerCustomShortcutSet(Shortcuts.CreateItem.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        final DirectoryDto pDir = tools.getCurrentSelectedDirectory(tree);
        final TreePath path = tree.getSelectionPath();

        if (path == null || pDir == null) return;

        final BiConsumer<String, DirectoryType> onCreate = (s, dt) -> {

            if (s.isEmpty()) return;
            final Path newDirPath = pDir.getPath().resolve(s);

            DirectoryDto dir = dt.getAction().apply(p).execute(s, pDir, newDirPath);
            Services.getInstance(p, ProjectPanel.class).getProjectTree().refresh();

            // Asynchronous creators (test runs) return null and run their own
            // follow-up once their dialog completes - including their own
            // confirmation, which is why this one sits after the null check.
            if (dir == null) return;

            Services.getInstance(p, Notifier.class).softShow(p, "Node created");

            if (dt == DirectoryType.TS)
                Services.getInstance(p, EditorUtil.class).open(p, dir);

            dt.getCodegen().execute(p, dir);

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

    @Override
    public void update(final @NotNull AnActionEvent e) {

        DirectoryDto parentDir = tools.getCurrentSelectedDirectory(tree);

        if (parentDir == null || parentDir instanceof TestProjectDirectoryDto) {
            e.getPresentation().setEnabled(false);
            return;
        }

        e.getPresentation().setEnabled(parentDir.canCreateChildren());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}
