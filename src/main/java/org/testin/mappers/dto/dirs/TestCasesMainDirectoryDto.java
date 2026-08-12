package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.CreateNodeMenu;
import org.testin.enums.DirectoryType;
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
    public @NotNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_CASES_MAIN_DIR;
    }

    @Override
    public @NotNull Object resolveDirectoryObject(final Path folder, final ProjectIndexer indexer) {
        return indexer.getTestSetByPath(folder);
    }


    @Override
    public boolean isRenamable() {
        return false;
    }

    @Override
    public boolean isTransferable() {
        return false;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @Override
    public boolean isTestCaseContainer() {
        return true;
    }

    @Override
    public @NotNull String getMarkerFileName() {
        return DirectoryType.TCD.getMarker();
    }

    @Override
    public boolean acceptsTransferred(final DirectoryDto source) {
        // Run nodes never land in the test-set family.
        return super.acceptsTransferred(source) && source.isAllowedInTestSetFamily();
    }

}
