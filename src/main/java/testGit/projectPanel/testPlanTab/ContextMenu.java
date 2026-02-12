package testGit.projectPanel.testPlanTab;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.treeStructure.SimpleTree;
import testGit.actions.AddTestPlan;
import testGit.actions.AddTestRun;
import testGit.actions.Delete;
import testGit.actions.RenameAction;
import testGit.projectPanel.ProjectPanel;


public class ContextMenu extends DefaultActionGroup {

    public ContextMenu(ProjectPanel projectPanel) {
        super("Test Plan Context", true);
        SimpleTree tree = projectPanel.getTestPlanTree();

        add(new AddPlanGroup(tree));
        addSeparator();
        add(new RenameAction(projectPanel, tree));
        add(new Delete(projectPanel, tree));
        addSeparator();
    }

    /**
     * كلاس داخلي لفصل منطق "Add" والتحكم في حالته (Disabled vs Enabled)
     */
    private static class AddPlanGroup extends DefaultActionGroup {
        public AddPlanGroup(SimpleTree tree) {
            super("Add", "Add test plan items", AllIcons.General.Add);
            setPopup(true);
            add(new AddTestPlan(tree));
            add(new AddTestRun(tree));
        }


        /**
         * دالة مساعدة لإنشاء المجموعات الفرعية (في حال احتجت لإضافة Import/Export لاحقاً)
         */
        private DefaultActionGroup createSubGroup(String title, javax.swing.Icon icon, AnAction... actions) {
            DefaultActionGroup group = new DefaultActionGroup(title, true);
            group.getTemplatePresentation().setIcon(icon);
            for (AnAction action : actions) {
                group.add(action);
            }
            return group;
        }

    }

}