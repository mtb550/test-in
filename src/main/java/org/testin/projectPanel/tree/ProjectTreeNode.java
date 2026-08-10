package org.testin.projectPanel.tree;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.ProjectStatus;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.services.Services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * IntelliJ tree node whose children are resolved by StructureTreeModel in the background.
 */
public final class ProjectTreeNode extends AbstractTreeNode<Object> {
    private final @NotNull Project project;

    public ProjectTreeNode(final @NotNull Project project, final Object value) {
        super(project, value);
        this.project = project;
    }

    @Override
    public @NotNull Collection<? extends AbstractTreeNode<?>> getChildren() {
        final Object value = getValue();
        if (value instanceof TestProjectDirectoryDto projectDirectory) {
            if (projectDirectory.getMarker().getStatus() != ProjectStatus.ACTIVE) return List.of();
            return List.of(
                    child(projectDirectory.getTestCasesDirectory()),
                    child(projectDirectory.getTestRunsDirectory())
            );
        }
        if (!(value instanceof DirectoryDto directory)) return List.of();

        try {
            final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);
            indexer.awaitIndexing();
            final List<ProjectTreeNode> children = new ArrayList<>();
            for (DirectoryDto child : indexer.getChildren(directory.getPath())) {
                children.add(child(child));
            }
            return children;
        } catch (final Exception ex) {
            Logger.error("Could not load tree children for '" + directory.getPath() + "': " + ex.getMessage());
            return List.of();
        }
    }

    private ProjectTreeNode child(final DirectoryDto value) {
        final ProjectTreeNode child = new ProjectTreeNode(project, value);
        child.setParent(this);
        return child;
    }

    @Override
    public LeafState getLeafState() {
        return getValue() instanceof DirectoryDto ? LeafState.ASYNC : LeafState.ALWAYS;
    }

    @Override
    protected void update(final @NotNull PresentationData presentation) {
        final Object value = getValue();
        presentation.setPresentableText(value instanceof DirectoryDto directory ? directory.getName() : String.valueOf(value));
    }
}
