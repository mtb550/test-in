package org.testin.projectPanel.projectSelector;

import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ListenerImpl implements ActionListener {
    private final @NotNull ProjectPanel pp;

    /** Null until the first selection; compared against to swallow repeat events. */
    private @Nullable TestProjectDirectoryDto lastSelected = null;

    public ListenerImpl(final @NotNull ProjectPanel pp) {
        this.pp = pp;
    }

    @Override
    public void actionPerformed(final @NotNull ActionEvent e) {
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