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
public class ContextMenu extends DefaultActionGroup {

    public ContextMenu(ProjectPanel projectPanel) {
        super("Tree Context Menu", true);

        SimpleTree tree = projectPanel.getProjectTree().getMainTree();

        add(new Open(projectPanel, tree));
        add(new AddGroup(projectPanel, tree));
        addSeparator();
        add(new Remove(tree));
        add(new Rename(projectPanel, tree));
        addSeparator();
        add(new RunTestSet(tree));
        addSeparator();
        add(createSubGroup("Export", AllIcons.ToolbarDecorator.Export, new ExportCsv(), new ExportHtml(), new ExportExcel(), new ExportJson()));
        add(createSubGroup("Import", AllIcons.ToolbarDecorator.Import, new ImportCsv(), new ImportExcel(projectPanel), new ImportJson()));
        add(createSubGroup("Integrate", AllIcons.Nodes.Related, new IntegrateTestRail(), new IntegrateJira(), new IntegrateAzure()));
        addSeparator();
        add(new OpenOldVersions());
        add(new ViewCommits());
        add(new TestRuns());
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

    private static class AddGroup extends DefaultActionGroup {

        public AddGroup(ProjectPanel projectPanel, SimpleTree tree) {
            super("Create", "Create new items", AllIcons.General.Add);
            setPopup(true);

            add(new CreateTestPackage(projectPanel, tree));
            add(new CreateTestSet(tree));
            //add(new CreateTestRunPackage(tree));
            add(new CreateTestRun(projectPanel));
        }


    }
}