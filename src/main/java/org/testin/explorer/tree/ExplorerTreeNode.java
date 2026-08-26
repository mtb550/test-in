package org.testin.explorer.tree;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.ProjectStatus;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.services.Services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * IntelliJ tree node whose children are resolved by StructureTreeModel in the background.
 */
public final class ExplorerTreeNode extends AbstractTreeNode<Object> {
    private final @NotNull Project p;

    public ExplorerTreeNode(final @NotNull Project p, final @NotNull Object value) {
        super(p, value);
        this.p = p;
    }

    @Override
    public @NotNull Collection<? extends AbstractTreeNode<?>> getChildren() {
        final @NotNull Object value = getValue();
        if (value instanceof TestProjectDirectoryDto projectDirectory) {
            if (projectDirectory.getMarker().getStatus() != ProjectStatus.ACTIVE) return List.of();
            return List.of(
                    child(projectDirectory.getTestCasesDirectory()),
                    child(projectDirectory.getTestRunsDirectory())
            );
        }
        if (!(value instanceof DirectoryDto directory)) return List.of();

        try {
            // Never waits for the index, and this is load-bearing rather than an
            // optimization. The platform calls this on the tree's Invoker inside
            // a read action, and a read action that blocks blocks every write
            // action in the IDE with it - including the one DumbService takes to
            // start indexing, which is the EDT's. Waiting here for 32 seconds
            // froze the whole IDE and killed it (#89).
            //
            // A node with nothing indexed under it yet answers with nothing, and
            // ExplorerPanel.refreshWhenIndexed draws it again when the index is
            // ready. That wait is on a pooled thread, holding no lock.
            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            final @NotNull List<ExplorerTreeNode> children = new ArrayList<>();
            for (final DirectoryDto child : indexer.getChildren(directory.getPath())) {
                children.add(child(child));
            }
            return children;
        } catch (final Exception ex) {
            final @NotNull String message = "Could not load '" + directory.getName() + "'";
            Logger.error(message + ": " + ex.getMessage());
            return List.of(child(new TreeLoadError(message)));
        }
    }

    private @NotNull ExplorerTreeNode child(final @NotNull Object value) {
        final @NotNull ExplorerTreeNode child = new ExplorerTreeNode(p, value);
        child.setParent(this);
        return child;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return getValue() instanceof DirectoryDto ? LeafState.ASYNC : LeafState.ALWAYS;
    }

    /**
     * The platform's expand-all asks every node this before descending into it.
     * An archived package answers no, so it stays collapsed when the tree opens
     * a project, while a click still expands it as before.
     */
    @Override
    public boolean isIncludedInExpandAll() {
        return !(getValue() instanceof DirectoryDto directory && directory.isRetired());
    }

    @Override
    protected void update(final @NotNull PresentationData presentation) {
        final @NotNull Object value = getValue();
        presentation.setPresentableText(value instanceof DirectoryDto directory ? directory.getName() : String.valueOf(value));
    }
}
