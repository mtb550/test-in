package org.testin.explorer.tree;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;

/**
 * Supplies the selected project hierarchy to IntelliJ's asynchronous tree models.
 */
public final class ExplorerTreeStructure extends AbstractTreeStructure {

    /**
     * The root the tree draws when the repository is bound to no project - a
     * word rather than a node, which is what the panel covers with its welcome
     * screen anyway.
     */
    private static final @NotNull String NO_PROJECT = "Project";

    private final @NotNull Project project;
    private volatile @NotNull ExplorerTreeNode root;

    public ExplorerTreeStructure(final @NotNull Project project,
                                final @NotNull Optional<TestProjectDirectoryDto> selectedProject) {
        this.project = project;
        this.root = createRoot(selectedProject);
    }

    public void setSelectedProject(final @NotNull Optional<TestProjectDirectoryDto> selectedProject) {
        root = createRoot(selectedProject);
    }

    private @NotNull ExplorerTreeNode createRoot(final @NotNull Optional<TestProjectDirectoryDto> selectedProject) {
        return new ExplorerTreeNode(project, selectedProject.map(Object.class::cast).orElse(NO_PROJECT));
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

    /**
     * The platform's contract: null is how a tree structure says "this is the
     * root", and AbstractTreeStructure reads it before we do (#71).
     */
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
