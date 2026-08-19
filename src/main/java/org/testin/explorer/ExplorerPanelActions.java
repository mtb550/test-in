package org.testin.explorer;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.creator.CreateTestProjectAction;
import org.testin.explorer.toolbar.CollapseAllAction;
import org.testin.explorer.toolbar.ExpandAllAction;
import org.testin.explorer.toolbar.RefreshAction;
import org.testin.setting.OpenSettingsAction;
import org.testin.testproject.SelectTestProjectAction;

import java.util.List;

public class ExplorerPanelActions {

    public @NotNull List<AnAction> create(final @NotNull Project p, final @NotNull ExplorerPanel pp) {
        return List.of(
                new OpenSettingsAction(p),
                new ExpandAllAction(pp),
                new CollapseAllAction(pp),
                new RefreshAction(p, pp),
                new SelectTestProjectAction(p, pp),
                new CreateTestProjectAction(p, pp)
        );
    }
}