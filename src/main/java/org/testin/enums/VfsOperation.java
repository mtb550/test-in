package org.testin.enums;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface VfsOperation {
    void execute(final @NotNull VirtualFile vf);
}
