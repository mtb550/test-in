package org.testin.util;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface VfsBiOperation {
    void execute(final @NotNull VirtualFile sourceVf, final @NotNull VirtualFile targetVf);
}
