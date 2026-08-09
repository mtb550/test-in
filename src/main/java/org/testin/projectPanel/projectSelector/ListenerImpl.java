package org.testin.projectPanel.projectSelector;

import com.intellij.openapi.ui.ComboBox;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ListenerImpl implements ActionListener {
    private final ProjectPanel pp;
    private TestProjectDirectoryDto lastSelected = null;

    public ListenerImpl(final ProjectPanel pp) {
        this.pp = pp;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() instanceof ComboBox<?> comboBox) {
            if (comboBox.getSelectedItem() instanceof TestProjectDirectoryDto selected) {

                if (selected.equals(lastSelected)) {
                    return;
                }

                if (pp.getTestProjectSelector().isLoading())
                    return;

                lastSelected = selected;

                Logger.info("Selection changed to: " + selected.getName());
                pp.getTestProjectSelector().filterByTestProject(selected);
            }
        }
    }
}