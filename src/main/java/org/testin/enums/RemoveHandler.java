package org.testin.enums;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.dirs.DirectoryDto;

@FunctionalInterface
public interface RemoveHandler {
    void remove(final @NotNull Project p, final @NotNull DirectoryDto dir);
}
