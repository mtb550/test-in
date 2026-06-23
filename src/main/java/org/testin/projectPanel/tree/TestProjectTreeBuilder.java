package org.testin.projectPanel.tree;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.projectPanel.ProjectPanel;

public class TestProjectTreeBuilder extends AbstractTreeBuilder {

    public TestProjectTreeBuilder(final @NotNull Project project, final ProjectPanel projectPanel) {
        super(project, projectPanel);
    }

}