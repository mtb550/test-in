package testGit.actions;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.mappers.TestRun;

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
            protected @NotNull JComponent createCenterPanel() {
                JBPanel<?> panel = new JBPanel<>(new GridLayout(0, 1, 6, 6));
                panel.setPreferredSize(new Dimension(400, 300));

                panel.add(new JBLabel("Name: " + run.getRunName()));


                return panel;
            }

            private String safe(String value) {
                return value != null ? value : "(not set)";
            }
        };

        dialog.show();
    }
}
