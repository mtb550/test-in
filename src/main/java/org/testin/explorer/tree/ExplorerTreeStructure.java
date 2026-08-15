package org.testin.explorer.tree;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;

/**
 * Supplies the selected project hierarchy to IntelliJ's asynchronous tree models.
 */
public final class ExplorerTreeStructure extends AbstractTreeStructure {
    private final @NotNull Project project;
    private volatile @NotNull ExplorerTreeNode root;

    public ExplorerTreeStructure(final @NotNull Project project, final @Nullable TestProjectDirectoryDto selectedProject) {
        this.project = project;
        this.root = createRoot(selectedProject);
    }

    public void setSelectedProject(final @Nullable TestProjectDirectoryDto selectedProject) {
        root = createRoot(selectedProject);
    }

    private @NotNull ExplorerTreeNode createRoot(final @Nullable TestProjectDirectoryDto selectedProject) {
        return new ExplorerTreeNode(project, selectedProject == null ? "Project" : selectedProject);
    }

    @Override
    public @NotNull Object getRootElement() {
        return root;
    }

    @Override
    public Object @NotNull [] getChildElements(final @NotNull Object element) {
        if (!(element instanceof ExplorerTreeNode node)) return new Object[0];
        return node.getChildren().toArray();
    }

    @Override
    public @Nullable Object getParentElement(final @NotNull Object element) {
        return element instanceof ExplorerTreeNode node ? node.getParent() : null;
    }

    @Override
    public @NotNull NodeDescriptor<?> createDescriptor(final @NotNull Object element,
                                                       final @Nullable NodeDescriptor parentDescriptor) {
        return (ExplorerTreeNode) element;
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean hasSomethingToCommit() {
        return false;
    }

    @Override
    public boolean isToBuildChildrenInBackground(final @NotNull Object element) {
        return true;
    }

    @Override
    public boolean isValid(final @NotNull Object element) {
        return element instanceof ExplorerTreeNode;
    }
}
