package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.CreateNodeMenu;
import org.testin.enums.DirectoryType;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.markers.TestSetPackageMarker;

import java.nio.file.Path;

@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TestSetPackageDirectoryDto extends DirectoryDto {
    @NonNull
    @Builder.Default
    private TestSetPackageMarker marker = new TestSetPackageMarker();

    @Override
    public @NotNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_SET_PACKAGE;
    }

    @Override
    public @Nullable Object resolveDirectoryObject(final @NotNull Path folder, final @NotNull ProjectIndexer indexer) {
        return indexer.getTestSetByPath(folder);
    }


    @Override
    public boolean isTestCaseContainer() {
        return true;
    }

    @Override
    public @NotNull String getMarkerFileName() {
        return DirectoryType.TSP.getMarker();
    }

    @Override
    public boolean acceptsTransferred(final @NotNull DirectoryDto source) {
        // Run nodes never land in the test-set family.
        return super.acceptsTransferred(source) && source.isAllowedInTestSetFamily();
    }

    @Override
    public boolean isAllowedInTestRunFamily() {
        return false;
    }

    @Override
    public @NotNull DirectoryType getType() {
        return DirectoryType.TSP;
    }
}
