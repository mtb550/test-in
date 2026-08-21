package org.testin.creator;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.DirectoryMapper;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.Optional;

@AllArgsConstructor
public class CreateTestRunPackage implements NodeCreator {
    private final @NotNull Project p;

    @Override
    public @NotNull Optional<DirectoryDto> execute(final @NotNull String name, final @NotNull DirectoryDto parentDir, final @NotNull Path newDirPath) {
        TestRunPackageDirectoryDto tr = Services.getInstance(p, DirectoryMapper.class).getTestRunPackageNode(p, newDirPath, parentDir);

        // The indexer owns all file/dir I/O: it creates the directory + .trp marker
        // (with JSON content) and registers the node.
        Services.getInstance(p, ProjectIndexer.class).addTestRunPackage(tr);

        return Optional.of(tr);
    }
}

