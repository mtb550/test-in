package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.CreateNodeMenu;
import org.testin.enums.DirectoryType;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.markers.TestRunsMainDirectoryMarker;

import java.nio.file.Path;

@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TestRunsMainDirectoryDto extends DirectoryDto {
    @NonNull
    @Builder.Default
    private TestRunsMainDirectoryMarker marker = new TestRunsMainDirectoryMarker();

    @Override
    public @NotNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_RUNS_MAIN_DIR;
    }

    @Override
    public @NotNull Object resolveDirectoryObject(final Path folder, final ProjectIndexer indexer) {
        return indexer.getTestRunDirByPath(folder);
    }


    @Override
    public boolean isRenamable() {
        return false;
    }

    @Override
    public String getMarkerFileName() {
        return DirectoryType.TRD.getMarker();
    }
}
