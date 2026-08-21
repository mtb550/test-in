package org.testin.creator;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.DirectoryMapper;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.Optional;

@AllArgsConstructor
public class CreateTestSetPackage implements NodeCreator {
    private final @NotNull Project p;

    @Override
    public @NotNull Optional<DirectoryDto> execute(final @NotNull String name, final @NotNull DirectoryDto parentDir, final @NotNull Path newDirPath) {
        TestSetPackageDirectoryDto tsp = Services.getInstance(p, DirectoryMapper.class).getTestSetPackageNode(p, newDirPath, parentDir);

        Services.getInstance(p, ProjectIndexer.class).addTestSetPackage(tsp);
        return Optional.of(tsp);
    }
}

