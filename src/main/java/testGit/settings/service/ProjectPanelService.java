package testGit.settings.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;
import testGit.projectPanel.ProjectPanel;

/**
 * Service to hold the reference of ProjectPanel so it can be
 * accessed from Settings or other background actions.
 */
@Service(Service.Level.PROJECT)
public final class ProjectPanelService {
    private ProjectPanel projectPanel;

    public static ProjectPanelService getInstance(Project project) {
        return project.getService(ProjectPanelService.class);
    }

    @Nullable
    public ProjectPanel getPanel() {
        return projectPanel;
    }

    public void setPanel(ProjectPanel panel) {
        this.projectPanel = panel;
    }
}
