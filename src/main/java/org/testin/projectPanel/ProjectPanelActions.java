package org.testin.projectPanel;

import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.NotNull;
import org.testin.projectPanel.toolBar.CollapseAllAction;
import org.testin.projectPanel.toolBar.ExpandAllAction;
import org.testin.projectPanel.toolBar.RefreshAction;
import org.testin.settings.OpenSettingsAction;
import org.testin.testProject.CreateTestProjectAction;

import java.util.List;

public class ProjectPanelActions {

    public static List<AnAction> create(final @NotNull ProjectPanel projectPanel) {
        return List.of(
                new OpenSettingsAction(),
                new ExpandAllAction(projectPanel),
                new CollapseAllAction(projectPanel),
                new RefreshAction(projectPanel),
                new CreateTestProjectAction(projectPanel)
        );
    }
}