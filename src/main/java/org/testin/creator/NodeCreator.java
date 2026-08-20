package org.testin.creator;

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.DirectoryDto;

import java.nio.file.Path;
import java.util.Optional;

@FunctionalInterface
public interface NodeCreator {

    /**
     * Creates the node and answers with it.
     * <p>
     * Empty for a creator that finishes asynchronously - {@link CreateTestRun}
     * completes after its dialog's OK and performs its own tree refresh, editor
     * opening and confirmation - and for one that cannot be started from the
     * tree at all. Either way there is no node yet for the caller's follow-up.
     */
    @NotNull
    Optional<DirectoryDto> execute(final @NotNull String name, final DirectoryDto parentDir, final @NotNull Path newDirPath);
}
