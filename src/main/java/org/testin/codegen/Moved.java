package org.testin.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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
     * <p>
     * An Optional rather than an empty list, because an empty list did not read
     * as "no destination" - it read as the default package. Neither mover
     * checked it, and {@code packageFolder} joins no segments into the empty
     * string, which resolves to the source root itself. So a tree that changed
     * underneath a move did not leave the code alone: it moved the generated
     * class, or a package folder with everything nested under it, into the
     * default package at the top of the source root and rewrote the package
     * declarations to match. The tree still named the old package, so the code
     * was never found again - every case under it reported "no generated code",
     * and a later rename or remove silently did nothing.
     * <p>
     * The type is the fix. There is no longer a value here that can be used by
     * accident; a caller has to say what it does when there is no destination.
     */
    public @NotNull Optional<List<String>> destinationPackage(final @NotNull Project p) {
        return Services.getInstance(p, ProjectIndexer.class).find(newParent).map(Fqcn::ofPackage);
    }
}
