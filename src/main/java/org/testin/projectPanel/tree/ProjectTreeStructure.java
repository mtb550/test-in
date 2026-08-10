package org.testin.projectPanel.tree;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;

/**
 * Supplies the selected project hierarchy to IntelliJ's asynchronous tree models.
 */
public final class ProjectTreeStructure extends AbstractTreeStructure {
    private final @NotNull Project project;
    private volatile @NotNull ProjectTreeNode root;

    public ProjectTreeStructure(final @NotNull Project project, final TestProjectDirectoryDto selectedProject) {
        this.project = project;
        this.root = createRoot(selectedProject);
    }

    public void setSelectedProject(final TestProjectDirectoryDto selectedProject) {
        root = createRoot(selectedProject);
    }

    private ProjectTreeNode createRoot(final TestProjectDirectoryDto selectedProject) {
        return new ProjectTreeNode(project, selectedProject == null ? "Project" : selectedProject);
    }

    @Override
    public Object getRootElement() {
        return root;
    }

    @Override
    public Object[] getChildElements(final Object element) {
        if (!(element instanceof ProjectTreeNode node)) return new Object[0];
        return node.getChildren().toArray();
    }

    @Override
    public Object getParentElement(final Object element) {
        return element instanceof ProjectTreeNode node ? node.getParent() : null;
    }

    @Override
    public NodeDescriptor<?> createDescriptor(final Object element, final NodeDescriptor parentDescriptor) {
        return (ProjectTreeNode) element;
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean hasSomethingToCommit() {
        return false;
    }

    @Override
    public boolean isToBuildChildrenInBackground(final Object element) {
        return true;
    }

    @Override
    public boolean isValid(final Object element) {
        return element instanceof ProjectTreeNode;
    }
}
