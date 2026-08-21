package org.testin.creator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.DirectoryMapper;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.Optional;

@AllArgsConstructor
public class CreateTestSet implements NodeCreator {
    private final @NotNull Project p;

    @Override
    public @NotNull Optional<DirectoryDto> execute(final @NotNull String name, final @NotNull DirectoryDto parentDir, final @NotNull Path newDirPath) {
        final @NotNull TestSetDirectoryDto ts = Services.getInstance(p, DirectoryMapper.class).getTestSetNode(p, newDirPath, parentDir);

        Services.getInstance(p, ProjectIndexer.class).addTestSet(ts);
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                GenType.CREATE_TEST_SET.getAction().execute(p, ts);
            } catch (final Exception ex) {
                Logger.error("Failed to create Java class: " + ex.getMessage());
            }
        });

        return Optional.of(ts);
    }

}

