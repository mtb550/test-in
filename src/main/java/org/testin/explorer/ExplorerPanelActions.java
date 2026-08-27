package org.testin.explorer;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.creator.CreateTestProjectAction;
import org.testin.explorer.toolbar.CollapseAllAction;
import org.testin.explorer.toolbar.ExpandAllAction;
import org.testin.setting.OpenSettingsAction;
import org.testin.testproject.SelectTestProjectAction;

import java.util.List;

public class ExplorerPanelActions {

    public @NotNull List<AnAction> create(final @NotNull Project p, final @NotNull ExplorerPanel pp) {
        return List.of(
                new OpenSettingsAction(p),
                new ExpandAllAction(pp),
                new CollapseAllAction(pp),
                pp.getRefreshAction(),
                new SelectTestProjectAction(p, pp),
                new CreateTestProjectAction(p, pp)
        );
    }
}