package org.testin.nodeCreator;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.DirectoryType;
import org.testin.mappers.dto.dirs.*;
import org.testin.nodeCreator.dialogs.CreateRunDialog;
import org.testin.nodeCreator.dialogs.CreateTestDialog;
import org.testin.projectPanel.ProjectPanel;
import org.testin.services.Services;
import org.testin.util.EditorUtil;
import org.testin.util.Shortcuts;
import org.testin.util.Tools;

import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public class CreateTreeNodeAction extends DumbAwareAction {

    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;
    private final @NotNull Tools tools;

    public CreateTreeNodeAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super("Create", "Create new node", AllIcons.General.Add);
        this.p = p;
        this.tree = tree;
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
            // follow-up once their dialog completes.
            if (dir == null) return;

            if (dt == DirectoryType.TS)
                Services.getInstance(p, EditorUtil.class).open(p, dir);

            dt.getCodeGenerator().execute(p, dir);

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

        e.getPresentation().setEnabled(!parentDir.getMenu().getAvailableOptions().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}
