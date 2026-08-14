package org.testin.nodeCreator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GeneratorType;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;

@AllArgsConstructor
public class CreateTestSet implements NodeCreator {
    private final @NotNull Project p;

    @Override
    public @NotNull DirectoryDto execute(final @NotNull String name, final DirectoryDto parentDir, final @NotNull Path newDirPath) {
        final TestSetDirectoryDto ts = Services.getInstance(p, DirectoryMapper.class).getTestSetNode(p, newDirPath, parentDir);

        Services.getInstance(p, ProjectIndexer.class).addTestSet(ts);
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                GeneratorType.CREATE_TEST_SET.getAction().execute(p, ts);
            } catch (final Exception ex) {
                Logger.error("Failed to create Java class: " + ex.getMessage());
            }
        });

        return ts;
    }

}

