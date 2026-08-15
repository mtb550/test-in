package org.testin.explorer;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.creator.CreateTestProjectAction;
import org.testin.explorer.toolBar.CollapseAllAction;
import org.testin.explorer.toolBar.ExpandAllAction;
import org.testin.explorer.toolBar.RefreshAction;
import org.testin.settings.OpenSettingsAction;

import java.util.List;

public class ProjectPanelActions {

    public @NotNull List<AnAction> create(final @NotNull Project p, final @NotNull ProjectPanel pp) {
        return List.of(
                new OpenSettingsAction(p),
                new ExpandAllAction(pp),
                new CollapseAllAction(pp),
                new RefreshAction(p, pp),
                new CreateTestProjectAction(p, pp)
        );
    }
}