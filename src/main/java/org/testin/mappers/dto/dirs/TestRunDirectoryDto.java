package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.CreateNodeMenu;
import org.testin.enums.DirectoryType;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.markers.TestRunMarker;

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
    public @NotNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_RUN;
    }

    @Override
    public @NotNull Object resolveDirectoryObject(final Path folder, final ProjectIndexer indexer) {
        throw new RuntimeException("Could not resolve directory " + folder + ", parent: " + getClass().getSimpleName());
    }

    @Override
    public boolean isOpenableInEditor() {
        return true;
    }

    @Override
    public String getMarkerFileName() {
        return DirectoryType.TR.getMarker();
    }
}
