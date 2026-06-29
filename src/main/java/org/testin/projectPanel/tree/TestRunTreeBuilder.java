package org.testin.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.ProjectStatus;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.logger.Log;

public class TestRunTreeBuilder extends AbstractTreeBuilder {

    public TestRunTreeBuilder(final @NotNull Project project, final ProjectPanel projectPanel) {
        super(project, projectPanel);
    }

    public void buildTree(final TestProjectDirectoryDto selectedTestProjectDirectory) {
        try {
            if (selectedTestProjectDirectory == null || selectedTestProjectDirectory.getMarker().getStatus() != ProjectStatus.ACTIVE) {
                this.rootNode = null;
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (projectPanel.getProjectTree() != null) {
                        projectPanel.getProjectTree().updateNodes();
                    }
                });
                return;
            }

            super.buildTree(selectedTestProjectDirectory.getTestRunsDirectory());

        } catch (Exception e) {
            Log.error("TestRunTreeBuilder.buildTree() error for directory '" + (selectedTestProjectDirectory != null ? selectedTestProjectDirectory.getName() : "null") + "': " + e.getMessage());
            this.rootNode = null;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (projectPanel.getProjectTree() != null) {
                    projectPanel.getProjectTree().updateNodes();
                }
            });
        }
    }

}