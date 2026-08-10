package org.testin.testRun;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestRunDto;
import org.testin.ui.dialogs.FramelessDialogWrapper;

import javax.swing.*;
import java.awt.*;

public class TestRunInfoPopupDialog {

    public void show(final @NotNull TestRunDto run) {
        FramelessDialogWrapper dialog = new FramelessDialogWrapper(true) {
            {
                setTitle("Test Run Info");
                initFrameless();
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
