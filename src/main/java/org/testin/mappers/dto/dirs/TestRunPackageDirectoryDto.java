package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.testin.enums.CreateNodeMenu;
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
    public @NonNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_RUN_PACKAGE;
    }

    @Override
    public @NonNull Object resolveDirectoryObject(final Path folder, final ProjectIndexer indexer) {
        throw new RuntimeException("Could not resolve directory " + folder + ", parent: " + getClass().getSimpleName());
    }

}
