package testGit.actions;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.TestRun;

import javax.swing.*;
import java.awt.*;

public class TestRunInfoPopup {

    public static void show(TestRun run) {
        DialogWrapper dialog = new DialogWrapper(true) {
            {
                init();
                setTitle("Test Run Info");
            }

            @Override
            protected @Nullable JComponent createCenterPanel() {
                JBPanel<?> panel = new JBPanel<>(new GridLayout(0, 1, 6, 6));
                panel.setPreferredSize(new Dimension(400, 300));

                //int count = new sql().get("SELECT COUNT(*) FROM nafath_tp WHERE plan_id = ?", run.getId())
                // .asType(Integer.class);

                panel.add(new JBLabel("📁 Name: " + run.getRunName()));
                //panel.add(new JBLabel("🧪 Number of Test Cases: " + count));
                //panel.add(new JBLabel("📌 Status: " + safe(TestRunStatus.labelFor(run.getStatus()))));
                //panel.add(new JBLabel("⏱ Last Execution: " + safe(run.getLast_execution())));
                //panel.add(new JBLabel("👤 Created By: " + safe(run.getCreated_by())));
                //panel.add(new JBLabel("📅 Created At: " + safe(run.getCreated_at())));
                //panel.add(new JBLabel("✏ Modified By: " + safe(run.getModified_by())));
                //panel.add(new JBLabel("📆 Modified At: " + safe(run.getModified_at())));
                //panel.add(new JBLabel("👥 Assigned To: " + safe(run.getAssigned_to())));

                return panel;
            }

            private String safe(String value) {
                return value != null ? value : "(not set)";
            }
        };

        dialog.show();
    }
}
