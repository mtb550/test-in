package org.testin.editor;

import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;

@Getter
public class UnifiedVirtualFile extends LightVirtualFile {

    private final @NotNull DirectoryDto dir;

    public UnifiedVirtualFile(final @NotNull DirectoryDto dir, final @NotNull EditorType ft) {
        super(dir.getName());
        this.dir = dir;
        this.setFileType(ft);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public @NotNull String getUrl() {
        return TestinFileSystem.PROTOCOL + ":///" + dir.getPath().toAbsolutePath().toString().replace("\\", "/");
    }

    @Override
    public @NotNull String getPath() {
        return dir.getPath().toAbsolutePath().toString();
    }

    /**
     * Testin's own file system, and the light file's default when the platform
     * cannot hand it over.
     * <p>
     * Never null, which is not what the lookup promises: {@code getFileSystem}
     * answers null for a protocol the manager has no entry for, and this method
     * is declared not-null and called by anything that meets one of these
     * files. The Database plugin does, while working out an editor tab's title,
     * and threw an IDE error report over a Testin tab - from a coroutine, so
     * the report named Testin and pointed at their code.
     * <p>
     * A compiler cannot see this. The annotation is rewritten into a throw by
     * the IDE's instrumenter, so it exists only in a running IDE, which is why
     * this survived every build.
     */
    @Override
    public @NotNull VirtualFileSystem getFileSystem() {
        final VirtualFileSystem registered = VirtualFileManager.getInstance().getFileSystem(TestinFileSystem.PROTOCOL);

        return registered != null ? registered : super.getFileSystem();
    }

    public @NotNull TestSetDirectoryDto getTestSet() {
        return (TestSetDirectoryDto) dir;
    }

    public @NotNull TestRunDirectoryDto getTestRun() {
        return (TestRunDirectoryDto) dir;
    }
}