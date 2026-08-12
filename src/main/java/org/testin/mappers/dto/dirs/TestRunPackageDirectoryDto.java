package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.CreateNodeMenu;
import org.testin.enums.DirectoryType;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.markers.TestRunPackageMarker;

import java.nio.file.Path;

@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TestRunPackageDirectoryDto extends DirectoryDto {
    @NonNull
    @Builder.Default
    private TestRunPackageMarker marker = new TestRunPackageMarker();

    @Override
    public @NotNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_RUN_PACKAGE;
    }

    @Override
    public @NotNull Object resolveDirectoryObject(final Path folder, final ProjectIndexer indexer) {
        throw new RuntimeException("Could not resolve directory " + folder + ", parent: " + getClass().getSimpleName());
    }


    @Override
    public @NotNull String getMarkerFileName() {
        return DirectoryType.TRP.getMarker();
    }

    @Override
    public boolean isAllowedInTestSetFamily() {
        return false;
    }

    @Override
    public boolean acceptsTransferred(final DirectoryDto source) {
        // Test-set nodes never land in the run family.
        return super.acceptsTransferred(source) && source.isAllowedInTestRunFamily();
    }

}
