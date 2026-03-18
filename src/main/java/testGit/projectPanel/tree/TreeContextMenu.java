package testGit.projectPanel.tree;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import testGit.actions.*;
import testGit.projectPanel.ProjectPanel;

import javax.swing.*;

@NoArgsConstructor
public class TreeContextMenu extends DefaultActionGroup {

    public TreeContextMenu(ProjectPanel projectPanel, SimpleTree tree) {
        super("Tree Context Menu", true);

        add(new Open(projectPanel, tree));
        add(new CreateTreeNode(projectPanel, tree));
        addSeparator();

        add(createSubGroup("Actions", AllIcons.Actions.Edit,
                new UndoNode(tree),
                new RedoNode(tree),
                new Remove(tree),
                new Rename(projectPanel, tree),
                new CopyNode(tree),
                new CutNode(tree),
                new PasteNode(tree)
        ));

        addSeparator();
        add(new RunTestSet(tree));
        addSeparator();

        add(createSubGroup("Export", AllIcons.ToolbarDecorator.Export,
                new ExportCsv(),
                new ExportHtml(),
                new ExportExcel(),
                new ExportJson()
        ));

        add(createSubGroup("Import", AllIcons.ToolbarDecorator.Import,
                new ImportCsv(),
                new ImportExcel(projectPanel, tree),
                new ImportJson()
        ));

        add(createSubGroup("Integrate", AllIcons.Nodes.Related,
                new IntegrateTestRail(),
                new IntegrateJira(),
                new IntegrateAzure()
        ));

        addSeparator();

        add(new OpenOldVersions());
        add(new ViewCommits());
        add(new TestRuns());
    }

    public static void registerShortcuts(final SimpleTree tree, TreeTransferHandler transferHandler, TreeContextMenu treeContextMenu) {
        new Escape(tree, transferHandler);
        new OpenNodeCM(tree, treeContextMenu);

    }

    private DefaultActionGroup createSubGroup(String title, Icon icon, AnAction... actions) {
        DefaultActionGroup group = new DefaultActionGroup(title, true);
        group.getTemplatePresentation().setIcon(icon);
        for (AnAction action : actions) {
            group.add(action);
        }
        return group;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}