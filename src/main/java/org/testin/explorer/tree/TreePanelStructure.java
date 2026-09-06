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
public final class TreePanelStructure extends AbstractTreeStructure {

    /**
     * The root the tree draws when the repository is bound to no project - a
     * word rather than a node, which is what the panel covers with its welcome
     * screen anyway.
     */
    private static final @NotNull String NO_PROJECT = "Project";

    private final @NotNull Project p;
    private volatile @NotNull TreePanelNode root;

    public TreePanelStructure(final @NotNull Project p, final @NotNull Optional<TestProjectDirectoryDto> selectedProject) {
        this.p = p;
        this.root = createRoot(selectedProject);
    }

    public void setSelectedProject(final @NotNull Optional<TestProjectDirectoryDto> selectedProject) {
        root = createRoot(selectedProject);
    }

    private @NotNull TreePanelNode createRoot(final @NotNull Optional<TestProjectDirectoryDto> selectedProject) {
        return new TreePanelNode(p, selectedProject.map(Object.class::cast).orElse(NO_PROJECT));
    }

    @Override
    public @NotNull Object getRootElement() {
        return root;
    }

    @Override
    public Object @NotNull [] getChildElements(final @NotNull Object element) {
        if (!(element instanceof TreePanelNode node)) return new Object[0];
        return node.getChildren().toArray();
    }

    /**
     * The platform's contract: null is how a tree structure says "this is the
     * root", and AbstractTreeStructure reads it before we do (#71).
     */
    @Override
    public @Nullable Object getParentElement(final @NotNull Object element) {
        return element instanceof TreePanelNode node ? node.getParent() : null;
    }

    @Override
    public @NotNull NodeDescriptor<?> createDescriptor(final @NotNull Object element, final @Nullable NodeDescriptor parentDescriptor) {
        return (TreePanelNode) element;
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
        return element instanceof TreePanelNode;
    }
}
