package org.testin.creator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.dto.dirs.DirectoryDto;

import java.nio.file.Path;

@FunctionalInterface
public interface NodeCreator {

    /**
     * Creates the node and returns its directory DTO.
     * <p>
     * May return {@code null} for creators that finish asynchronously
     * (e.g. {@link CreateTestRun}, which completes after its dialog's OK and
     * performs its own tree refresh and editor opening). Callers must guard
     * their follow-up steps accordingly.
     */
    @Nullable
    DirectoryDto execute(final @NotNull String name, final DirectoryDto parentDir, final @NotNull Path newDirPath);
}
