package org.testin.editorPanel.toolBar;

import com.intellij.openapi.project.Project;
import org.testin.editorPanel.toolBar.components.*;

import java.util.List;

public class RunToolBar extends AbstractToolbarPanel {
    final Project project;

    public RunToolBar(Project project, final IToolBar callbacks) {
        super(callbacks);
        this.project = project;
        layoutComponents();
    }

    @Override
    public List<IToolbarItem> getCustomComponents() {
        return List.of(
                new StartExecutionBtn(getCallbacks(), getCallbacks()::onStartExecutionClicked),
                new GenerateReportBtn(project),
                new RefreshBtn(getCallbacks()::onToolBarRefreshButtonClicked),
                new RunDetailsPopup(getCallbacks()::onToolBarDetailsSelectionChanged),
                new FilterPopup(getCallbacks(), getCallbacks()::onToolBarFilterResetButtonClicked, getCallbacks()::onToolBarFilterSelectionChanged, getCallbacks()::getAvailableModules)
        );
    }
}