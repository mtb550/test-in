package testGit.projectPanel;

import com.intellij.openapi.actionSystem.AnAction;
import testGit.actions.*;

import java.util.List;

public class ProjectPanelActions {

    public static List<AnAction> create(ProjectPanel projectPanel) {
        return List.of(
                new OpenSettings(),
                new ExpandAll(projectPanel),
                new CollapseAll(projectPanel),
                new Refresh(projectPanel),
                new CreateTestProject(projectPanel)
        );
    }
}