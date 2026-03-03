package testGit.projectPanel.testCaseTab;

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
        super("Test Case Context", true);

        SimpleTree testCaseTree = projectPanel.getTestCaseTabController().getTree();

        add(new OpenTestSet2(testCaseTree));
        add(new AddGroup(testCaseTree));
        addSeparator();
        add(new DeletePackage(testCaseTree));
        add(new Rename(projectPanel, testCaseTree));
        addSeparator();
        add(new Run(testCaseTree));
        addSeparator();
        add(createSubGroup("Export", AllIcons.ToolbarDecorator.Export, new ExportCsv(), new ExportHtml(), new ExportExcel(), new ExportJson()));
        add(createSubGroup("Import", AllIcons.ToolbarDecorator.Import, new ImportCsv(), new ImportExcel(), new ImportJson()));
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

        public AddGroup(SimpleTree tree) {
            super("Create", "Create new items", AllIcons.General.Add);
            setPopup(true);

            add(new CreateTestCasePackage(tree));
            add(new CreateTestSet(tree));
        }


    }
}