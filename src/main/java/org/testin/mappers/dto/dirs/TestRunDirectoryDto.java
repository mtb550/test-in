package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.CreateNodeMenu;
import org.testin.mappers.markers.TestRunMarker;
import org.testin.util.indexer.ProjectIndexer;

import java.nio.file.Path;

@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TestRunDirectoryDto extends DirectoryDto {

    @NotNull
    @Builder.Default
    private TestRunMarker marker = new TestRunMarker();

    @Override
    public @NonNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_RUN;
    }

    @Override
    public @NonNull Object resolveDirectoryObject(final Path folder, final ProjectIndexer indexer) {
        throw new RuntimeException("Could not resolve directory " + folder + ", parent: " + getClass().getSimpleName());
    }
}
