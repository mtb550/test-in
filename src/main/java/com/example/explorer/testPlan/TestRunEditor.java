package com.example.explorer.testPlan;

import com.example.editor.TestCaseCard;
import com.example.pojo.TestCase;
import com.example.util.sql;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class TestRunEditor {

    private static final Map<String, LightVirtualFile> openTabs = new HashMap<>();

    public static void open(Project project, int testRunId, String title) {
        String tabTitle = "Test Run: " + title;

        // 🧠 Reuse if already open
        if (openTabs.containsKey(tabTitle)) {
            VirtualFile file = openTabs.get(tabTitle);
            new OpenFileDescriptor(project, file).navigate(true);
            return;
        }

        // 1. Build the UI content
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        TestCase[] testCases = new sql().get("""
                    SELECT t.* FROM nafath_tc t
                    JOIN nafath_tp_testcases map ON map.test_case_id = t.tc_id
                    WHERE map.plan_id = ?
                    ORDER BY map.run_order
                """, testRunId).as(TestCase[].class);

        for (int i = 0; i < testCases.length; i++) {
            panel.add(new TestCaseCard(i, testCases[i]));
            panel.add(Box.createVerticalStrut(8));
        }

        JBScrollPane scrollPane = new JBScrollPane(panel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // 2. Create a unique virtual file and register
        LightVirtualFile file = new LightVirtualFile(tabTitle);
        openTabs.put(tabTitle, file);  // 👈 remember it's open

        TestRunEditorService.getInstance(project).registerEditorComponent(file, scrollPane);

        // 3. Open it
        FileEditorManager.getInstance(project).openFile(file, true, true);
    }
}
