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

@NoArgsConstructor
public class ContextMenu extends DefaultActionGroup {

    public ContextMenu(ProjectPanel projectPanel) {
        // نص المجموعة الرئيسي (يظهر في الإعدادات أو القوائم المتداخلة)
        super("Test Case Context", true);

        SimpleTree tree = projectPanel.getTestCaseTree();

        add(new OpenFeature(tree));
        add(new AddGroup(tree));
        addSeparator();
        add(new Delete(projectPanel, tree));
        add(new RenameAction(projectPanel, tree));
        addSeparator();
        add(new RunAction(tree));
        addSeparator();
        add(createSubGroup("Export", AllIcons.ToolbarDecorator.Export, new ExportCsv(), new ExportHtml(), new ExportExcel(), new ExportJsonAction()));
        add(createSubGroup("Import", AllIcons.ToolbarDecorator.Import, new ImportCsvAction(), new ImportExcelAction(), new ImportJsonAction()));
        add(createSubGroup("Integrate", AllIcons.Nodes.Related, new IntegrateTestRail(), new IntegrateJira(), new IntegrateAzureAction()));
        addSeparator();
        add(new OpenOldVersions());
        add(new ViewCommitsAction());
        add(new TestPlansAction());
    }

    /**
     * دالة مساعدة لإنشاء المجموعات الفرعية بأيقونات وبسطر واحد
     */
    private DefaultActionGroup createSubGroup(String title, javax.swing.Icon icon, com.intellij.openapi.actionSystem.AnAction... actions) {
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

    /**
     * كلاس داخلي لمجموعة "Add" للتحكم في الأيقونة وحالة الظهور (Validation)
     */
    private static class AddGroup extends DefaultActionGroup {

        public AddGroup(SimpleTree tree) {
            super("Add", "Add new items", AllIcons.General.Add);
            setPopup(true);

            add(new AddModule(tree));
            add(new AddTestSet(tree));
        }


    }
}