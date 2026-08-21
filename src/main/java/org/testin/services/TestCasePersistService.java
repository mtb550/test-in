package org.testin.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;

import java.nio.file.Path;
import java.util.List;

@Service(Service.Level.PROJECT)
@AllArgsConstructor
public final class TestCasePersistService implements Disposable {
    private final @NotNull Project p;

    public void persist(final @NotNull Path path, final @NotNull List<TestCaseDto> tcs) {
        if (tcs.isEmpty()) return;

        ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() ->
                tcs.forEach(tc -> Services.getInstance(p, ProjectIndexer.class).putTestCase(path, tc))));
    }

    @Override
    public void dispose() {
        // todo, to be implemented
    }
}

