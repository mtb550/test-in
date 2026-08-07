package org.testin.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.ProjectStatus;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.logger.Logger;

public class TestRunTreeBuilder extends AbstractTreeBuilder {

    public TestRunTreeBuilder(final @NotNull Project p, final ProjectPanel pp) {
        super(p, pp);
    }

    public void buildTree(final TestProjectDirectoryDto selectedTestProjectDirectory) {
        try {
            if (selectedTestProjectDirectory == null || selectedTestProjectDirectory.getMarker().getStatus() != ProjectStatus.ACTIVE) {
                this.rootNode = null;
                ApplicationManager.getApplication().invokeLater(() -> pp.getProjectTree().refreshTree());
                return;
            }

            super.buildTree(selectedTestProjectDirectory.getTestRunsDirectory());

        } catch (final Exception ex) {
            Logger.error("TestRunTreeBuilder.buildTree() error for directory '" + (selectedTestProjectDirectory != null ? selectedTestProjectDirectory.getName() : "null") + "': " + ex.getMessage());
            this.rootNode = null;
            ApplicationManager.getApplication().invokeLater(() -> pp.getProjectTree().refreshTree());
        }
    }

}