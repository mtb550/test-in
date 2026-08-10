package org.testin.nodeCreator;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;

public class CreateTestSetPackage implements NodeCreator {
    private final @NotNull Project p;

    public CreateTestSetPackage(final @NotNull Project p) {
        this.p = p;
    }

    @Override
    public @NonNull DirectoryDto execute(final @NonNull String name, final DirectoryDto parentDir, final @NonNull Path newDirPath) {
        TestSetPackageDirectoryDto tsp = Services.getInstance(p, DirectoryMapper.class).getTestSetPackageNode(p, newDirPath, parentDir);

        Services.getInstance(p, ProjectIndexer.class).addTestSetPackage(tsp);
        return tsp;
    }
}
