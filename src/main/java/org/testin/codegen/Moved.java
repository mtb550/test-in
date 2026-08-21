package org.testin.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.List;

/**
 * A node and the directory it is about to sit in.
 * <p>
 * The companion of {@link Renamed}, for the other operation a
 * {@link GenAction}'s single object cannot carry alone: the node as it still is,
 * so its generated code is found where it currently lives, and where it is
 * going, so the destination package can be worked out.
 * <p>
 * The node has not moved yet when this is built. The order matters: the Java is
 * moved first, while the old path is still what finds it.
 * <p>
 * The parent rather than the node's new full path, because a move never changes
 * a node's name - and because a path with no parent is not a state any caller
 * should have to consider.
 */
public record Moved(@NotNull DirectoryDto dir, @NotNull Path newParent) {

    /**
     * The package the destination stands for, as its segments.
     * <p>
     * Asked of the indexer rather than worked out from the path, because a
     * package name comes from the tree the tester sees and not from the folder
     * names on disk - the Test Cases node, for one, is a place in the tree and
     * never a package.
     * <p>
     * Empty when the destination is not indexed, which means the tree changed
     * underneath the move; the movers leave the code alone rather than guess.
     */
    public @NotNull List<String> destinationPackage(final @NotNull Project p) {
        return Services.getInstance(p, ProjectIndexer.class).find(newParent)
                .map(Fqcn::ofPackage)
                .orElse(List.of());
    }
}
