package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.CreateNodeMenu;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.markers.TestProjectMarker;

import java.nio.file.Path;

@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TestProjectDirectoryDto extends DirectoryDto {

    @NotNull
    @Builder.Default
    private TestCasesMainDirectoryDto testCasesDirectory = new TestCasesMainDirectoryDto();

    @NotNull
    @Builder.Default
    private TestRunsMainDirectoryDto testRunsDirectory = new TestRunsMainDirectoryDto();

    @NotNull
    private String pathName;

    @NotNull
    @Builder.Default
    private TestProjectMarker marker = new TestProjectMarker();

    @Override
    public @NotNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_PROJECT;
    }

    @Override
    public @Nullable Object resolveDirectoryObject(final Path folder, final ProjectIndexer indexer) {
        return indexer.getTestSetPackageByPath(folder);
    }

}
