package org.testin.nodeCreator;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;

@AllArgsConstructor
public class CreateTestSetPackage implements NodeCreator {
    private final @NotNull Project p;

    @Override
    public @NotNull DirectoryDto execute(final @NotNull String name, final DirectoryDto parentDir, final @NotNull Path newDirPath) {
        TestSetPackageDirectoryDto tsp = Services.getInstance(p, DirectoryMapper.class).getTestSetPackageNode(p, newDirPath, parentDir);

        Services.getInstance(p, ProjectIndexer.class).addTestSetPackage(tsp);
        return tsp;
    }
}

