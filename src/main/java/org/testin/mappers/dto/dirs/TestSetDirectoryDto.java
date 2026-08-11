package org.testin.mappers.dto.dirs;

import org.jetbrains.annotations.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.CreateNodeMenu;
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
    public @NotNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_SET;
    }

    @Override
    public @Nullable Object resolveDirectoryObject(final Path folder, final ProjectIndexer indexer) {
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
}
