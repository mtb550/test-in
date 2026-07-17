package org.testin.editorPanel;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem;
import com.intellij.openapi.vfs.NonPhysicalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.services.Services;

import java.nio.file.Path;

public final class TestinFileSystem extends DeprecatedVirtualFileSystem implements NonPhysicalFileSystem {

    public static final String PROTOCOL = "testin";

    @Override
    public @NonNls @NotNull String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public @Nullable VirtualFile findFileByPath(final @NotNull String path) {
        final Path dirPath = Path.of(path);

        for (final Project project : ProjectManager.getInstance().getOpenProjects()) {
            final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);

            DirectoryDto dir = indexer.getTestSetByPath(dirPath);
            if (dir == null) {
                dir = indexer.getTestRunDirByPath(dirPath);
            }

            if (dir != null) {
                final FileType ft = dir instanceof TestRunDirectoryDto ? FileType.TEST_RUN : FileType.TEST_CASE;
                return new UnifiedVirtualFile(dir, ft);
            }
        }

        return null;
    }

    @Override
    public void refresh(final boolean asynchronous) {

    }

    @Override
    public @Nullable VirtualFile refreshAndFindFileByPath(final @NotNull String path) {
        return findFileByPath(path);
    }
}