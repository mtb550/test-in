package org.testin.nodeCreator;

import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.dirs.DirectoryDto;
import java.nio.file.Path;

@FunctionalInterface
public interface NodeCreator {
    @NotNull DirectoryDto execute(final @NotNull String name, final DirectoryDto parentDir, final @NotNull Path newDirPath);
}
