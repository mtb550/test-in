package org.testin.nodeCreator;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;

@AllArgsConstructor
public class CreateTestRunPackage implements NodeCreator {
    private final @NotNull Project p;

    @Override
    public @NotNull DirectoryDto execute(final @NotNull String name, final DirectoryDto parentDir, final @NotNull Path newDirPath) {
        TestRunPackageDirectoryDto tr = Services.getInstance(p, DirectoryMapper.class).getTestRunPackageNode(p, newDirPath, parentDir);

        // The indexer owns all file/dir I/O: it creates the directory + .trp marker
        // (with JSON content) and registers the node.
        Services.getInstance(p, ProjectIndexer.class).addTestRunPackage(tr);

        return tr;
    }
}

