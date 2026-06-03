package org.testin.projectPanel.projectSelector;

import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.logger.Log;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ListenerImpl implements ActionListener {
    private final ProjectPanel projectPanel;
    private TestProjectDirectoryDto lastSelected = null;

    public ListenerImpl(final ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() instanceof JComboBox<?> comboBox) {
            if (comboBox.getSelectedItem() instanceof TestProjectDirectoryDto selected) {

                if (selected.equals(lastSelected)) {
                    return;
                }

                lastSelected = selected;

                if (projectPanel.getTestProjectSelector() != null) {
                    Log.info("Selection changed to: " + selected.getName());
                    projectPanel.getTestProjectSelector().filterByTestProject(selected);
                }
            }
        }
    }
}