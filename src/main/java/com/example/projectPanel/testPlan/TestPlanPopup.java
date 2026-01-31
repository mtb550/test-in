package com.example.projectPanel.testPlan;

import com.example.pojo.TestPlan;
import com.intellij.ui.treeStructure.SimpleTree;

public class TestPlanPopup {
    public static void showFolderInfo(TestPlan plan, SimpleTree parent) {
        new TestPlanDialog(plan, parent).show();
    }
}