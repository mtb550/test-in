package org.testin.editor;

import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem;
import com.intellij.openapi.vfs.NonPhysicalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TestinFileSystem extends DeprecatedVirtualFileSystem implements NonPhysicalFileSystem {

    public static final @NotNull String PROTOCOL = "testin";

    @Override
    public @NonNls @NotNull String getProtocol() {
        return PROTOCOL;
    }

    /**
     * Nothing, always, and it cannot be otherwise - which is worth writing down
     * because the obvious improvement is to make it answer (#160).
     * <p>
     * It cannot. A file system is application level, so there is no project here
     * to ask, and a node's kind lives in a marker file on disk that only the
     * indexer is allowed to read. The one caller that ever asked was the IDE
     * restoring its tab list, and it asked twenty seconds before the index
     * existed. Testin closes its own tabs before that list is written now, so
     * nobody asks at all.
     */
    // The platform's own signature: a file system answers null for a path it
    // does not have, and the platform reads that before we do (#71).
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