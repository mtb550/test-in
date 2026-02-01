package testGit.projectPanel.testPlan;

import com.intellij.ui.treeStructure.SimpleTree;
import testGit.pojo.TestPlan;

public class TestPlanPopup {
    public static void showFolderInfo(TestPlan plan, SimpleTree parent) {
        new TestPlanDialog(plan, parent).show();
    }
}