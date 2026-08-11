package org.testin.mappers.dto.dirs;

import org.jetbrains.annotations.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.testin.enums.CreateNodeMenu;
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
    public @NotNull Object resolveDirectoryObject(final Path folder, final ProjectIndexer indexer) {
        return indexer.getTestSetByPath(folder);
    }


    @Override
    public boolean isTestCaseContainer() {
        return true;
    }
}
