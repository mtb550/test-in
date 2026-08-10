package org.testin.testRun;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestRunDto;

import javax.swing.*;
import java.awt.*;

public class TestRunInfoPopupDialog {

    public void show(final @NotNull TestRunDto run) {
        DialogWrapper dialog = new DialogWrapper(true) {
            {
                init();
                setTitle("Test Run Info");
            }

            @Override
            protected @NotNull JComponent createCenterPanel() {
                JBPanel<?> panel = new JBPanel<>(new GridLayout(0, 1, 6, 6));
                panel.setPreferredSize(new Dimension(400, 300));

                panel.add(new JBLabel("Name: " + run.getChangeLog()));

                return panel;
            }

        };

        dialog.show();
    }
}
