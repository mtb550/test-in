package org.testin.explorer;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.creator.CreateTestProjectAction;
import org.testin.explorer.toolbar.CollapseAllAction;
import org.testin.explorer.toolbar.ExpandAllAction;
import org.testin.search.SearchAction;
import org.testin.setting.OpenSettingsAction;
import org.testin.testproject.SelectTestProjectAction;

import java.util.List;

public class TreePanelActions {

    public @NotNull List<AnAction> create(final @NotNull Project p, final @NotNull TreePanel tp) {
        return List.of(
                // The keystroke reaches the search from anywhere, which is the
                // point of it - and is also why nothing on screen says the
                // search exists. The button is where a tester finds out.
                SearchAction.registered(),
                new OpenSettingsAction(p),
                new ExpandAllAction(tp),
                new CollapseAllAction(tp),
                tp.getRefreshAction(),
                new SelectTestProjectAction(p, tp),
                new CreateTestProjectAction(p, tp)
        );
    }
}