package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.testin.enums.CreateNodeMenu;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.markers.TestCasesMainDirectoryMarker;

import java.nio.file.Path;

@Setter
@Getter
@NoArgsConstructor
@ToString(callSuper = true)
@SuperBuilder
public class TestCasesMainDirectoryDto extends DirectoryDto {
    @NonNull
    @Builder.Default
    private TestCasesMainDirectoryMarker marker = new TestCasesMainDirectoryMarker();

    @Override
    public @NonNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_CASES_MAIN_DIR;
    }

    @Override
    public @NonNull Object resolveDirectoryObject(final Path folder, final ProjectIndexer indexer) {
        return indexer.getTestSetByPath(folder);
    }

}
