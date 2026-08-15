package org.testin.explorer.projectSelector;

import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.explorer.ExplorerPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ProjectSelectionListener implements ActionListener {
    private final @NotNull ExplorerPanel pp;

    /**
     * Null until the first selection; compared against to swallow repeat events.
     */
    private @Nullable TestProjectDirectoryDto lastSelected = null;

    public ProjectSelectionListener(final @NotNull ExplorerPanel pp) {
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