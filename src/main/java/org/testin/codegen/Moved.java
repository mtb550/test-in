/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;

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
     * UC-CODEGEN-016, Rule-CODEGEN-055.
     * <p>
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

    /**
     * UC-CODEGEN-016, Rule-CODEGEN-055.
     * <p>
     * Says that the node moved and its generated code did not.
     * <p>
     * Leaving the code alone is right - guessing a destination the tree has not
     * read is what once moved a whole package into the default package and lost
     * it - but doing it in silence is not. The tree and the code disagree from
     * this moment, and the only sign of it was a line in the log; the tester
     * found out when every case under the node reported no generated code
     * (#247).
     * <p>
     * One sentence for both movers, because a class left behind and a package
     * left behind are the same news to a tester and used to be phrased
     * separately. A notification that stays rather than a balloon that fades: it
     * needs acting on, and the move it follows may have been one of many.
     */
    public void reportCodeLeftBehind(final @NotNull Project p, final @NotNull String fullName) {
        Logger.warn("Destination is not indexed, so " + fullName + " is left where it is");

        Services.getInstance(p, Notifier.class).warn(p,
                Bundle.message("codegen.moved.title", dir.getName()),
                Bundle.message("codegen.moved.message", fullName));
    }
}
