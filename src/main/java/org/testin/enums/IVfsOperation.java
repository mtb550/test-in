package org.testin.enums;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface IVfsOperation {
    void execute(final @NotNull VirtualFile vf);
}
