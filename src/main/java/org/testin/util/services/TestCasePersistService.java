package org.testin.util.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.Service.Level;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Mapper;
import org.testin.util.notifications.Notifier;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service(Level.PROJECT)
public final class TestCasePersistService implements Disposable {
    public static TestCasePersistService getInstance(final Project project) {
        return project.getService(TestCasePersistService.class);
    }

    public void persist(final Path path, final @Nullable List<TestCaseDto> tcs) {
        if (path == null || tcs == null || tcs.isEmpty()) return;

        ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                VirtualFile dirVFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(path.toFile());
                if (dirVFile == null) {
                    Notifier.getInstance().error("Save Error", "Could not resolve directory: " + path);
                    return;
                }

                for (TestCaseDto tc : tcs) {
                    if (tc == null) continue;

                    String fileName = tc.getId() + ".json";
                    VirtualFile targetFile = dirVFile.findChild(fileName);

                    if (targetFile == null) {
                        targetFile = dirVFile.createChildData(this, fileName);
                    }

                    byte[] jsonBytes = Mapper.writeValueAsBytes(tc);
                    targetFile.setBinaryContent(jsonBytes);
                }

            } catch (IOException e) {
                Notifier.getInstance().error("Save Error", "Failed to persist data: " + e.getMessage());
            }
        }));
    }

    @Override
    public void dispose() {
        // todo, to be implemented
    }
}