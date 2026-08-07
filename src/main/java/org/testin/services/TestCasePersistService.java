package org.testin.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.util.Mapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class TestCasePersistService implements Disposable {
    private final @NotNull Project p;

    public TestCasePersistService(final @NotNull Project p) {
        this.p = p;
    }

    public void persist(final Path path, final @Nullable List<TestCaseDto> tcs) {
        if (path == null || tcs == null || tcs.isEmpty()) return;

        ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                VirtualFile dirVFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(path.toFile());
                if (dirVFile == null) {
                    Services.getInstance(p, Notifier.class).error(p, "Save Error", "Could not resolve directory: " + path);
                    return;
                }

                for (TestCaseDto tc : tcs) {
                    if (tc == null) continue;

                    Services.getInstance(p, ProjectIndexer.class).putTestCase(path, tc);

                    String fileName = tc.getId() + ".json";
                    VirtualFile targetFile = dirVFile.findChild(fileName);

                    if (targetFile == null) {
                        targetFile = dirVFile.createChildData(this, fileName);
                    }

                    byte[] jsonBytes = Services.getInstance(p, Mapper.class).writeValueAsBytes(tc);
                    targetFile.setBinaryContent(jsonBytes);
                }

            } catch (final IOException ex) {
                Services.getInstance(p, Notifier.class).error(p, "Save Error", "Failed to persist data: " + ex.getMessage());
            }
        }));
    }

    @Override
    public void dispose() {
        // todo, to be implemented
    }
}
