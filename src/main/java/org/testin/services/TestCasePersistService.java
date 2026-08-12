package org.testin.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.dto.TestCaseDto;

import java.nio.file.Path;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class TestCasePersistService implements Disposable {
    private final @NotNull Project p;

    public TestCasePersistService(final @NotNull Project p) {
        this.p = p;
    }

    public void persist(final @Nullable Path path, final @Nullable List<TestCaseDto> tcs) {
        if (path == null || tcs == null || tcs.isEmpty()) return;

        ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
            for (final TestCaseDto tc : tcs) {
                if (tc == null) continue;
                Services.getInstance(p, ProjectIndexer.class).putTestCase(path, tc);
            }
        }));
    }

    @Override
    public void dispose() {
        // todo, to be implemented
    }
}
