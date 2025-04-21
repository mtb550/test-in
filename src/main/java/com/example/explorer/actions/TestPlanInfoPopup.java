package com.example.explorer.actions;

import com.example.pojo.TestPlan;
import com.example.pojo.TestPlanStatus;
import com.example.util.sql;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class TestPlanInfoPopup {

    public static void show(TestPlan plan) {
        DialogWrapper dialog = new DialogWrapper(true) {
            {
                init();
                setTitle("Test Plan Info");
            }

            @Override
            protected @Nullable JComponent createCenterPanel() {
                JBPanel<?> panel = new JBPanel<>(new GridLayout(0, 1, 6, 6));
                panel.setPreferredSize(new Dimension(400, 300));

                int count = new sql().get("SELECT COUNT(*) FROM nafath_tp_testcases WHERE plan_id = ?", plan.getId())
                        .asType(Integer.class);

                panel.add(new JBLabel("📁 Name: " + plan.getName()));
                panel.add(new JBLabel("🧪 Number of Test Cases: " + count));
                panel.add(new JBLabel("📌 Status: " + safe(TestPlanStatus.labelFor(plan.getStatus()))));
                panel.add(new JBLabel("⏱ Last Execution: " + safe(plan.getLast_execution())));
                panel.add(new JBLabel("👤 Created By: " + safe(plan.getCreated_by())));
                panel.add(new JBLabel("📅 Created At: " + safe(plan.getCreated_at())));
                panel.add(new JBLabel("✏ Modified By: " + safe(plan.getModified_by())));
                panel.add(new JBLabel("📆 Modified At: " + safe(plan.getModified_at())));
                panel.add(new JBLabel("👥 Assigned To: " + safe(plan.getAssigned_to())));

                return panel;
            }

            private String safe(String value) {
                return value != null ? value : "(not set)";
            }
        };

        dialog.show();
    }
}
