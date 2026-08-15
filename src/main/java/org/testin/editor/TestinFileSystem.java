package org.testin.editor;

import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem;
import com.intellij.openapi.vfs.NonPhysicalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TestinFileSystem extends DeprecatedVirtualFileSystem implements NonPhysicalFileSystem {

    public static final String PROTOCOL = "testin";

    @Override
    public @NonNls @NotNull String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public @Nullable VirtualFile findFileByPath(final @NotNull String path) {
        return null;
    }

    @Override
    public void refresh(final boolean asynchronous) {

    }

    @Override
    public @Nullable VirtualFile refreshAndFindFileByPath(final @NotNull String path) {
        return null;
    }
}