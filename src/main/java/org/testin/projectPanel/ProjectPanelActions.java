package org.testin.projectPanel;

import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.*;

import java.util.List;

public class ProjectPanelActions {

    public static List<AnAction> create(final @NotNull ProjectPanel projectPanel) {
        return List.of(
                new OpenSettings(),
                new ExpandAll(projectPanel),
                new CollapseAll(projectPanel),
                new Refresh(projectPanel),
                new CreateTestProject(projectPanel)
        );
    }
}