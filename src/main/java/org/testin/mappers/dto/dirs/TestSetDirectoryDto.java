package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.DirectoryType;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.markers.TestSetMarker;

import java.nio.file.Path;

@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TestSetDirectoryDto extends DirectoryDto {
    @NonNull
    @Builder.Default
    private TestSetMarker marker = new TestSetMarker();


    @Override
    public @Nullable Object resolveDirectoryObject(final @NotNull Path folder, final @NotNull ProjectIndexer indexer) {
        return indexer.getTestSetByPath(folder);
    }


    @Override
    public boolean isTestCaseContainer() {
        return true;
    }

    @Override
    public boolean isOpenableInEditor() {
        return true;
    }

    @Override
    public @NotNull String getMarkerFileName() {
        return DirectoryType.TS.getMarker();
    }

    @Override
    public boolean isTransferTarget() {
        // A test set holds test cases only - no directory node ever lands
        // inside it (not a package, not another test set, not run nodes).
        return false;
    }

    @Override
    public boolean isAllowedInTestRunFamily() {
        return false;
    }

    @Override
    public @NotNull DirectoryType getType() {
        return DirectoryType.TS;
    }
}
