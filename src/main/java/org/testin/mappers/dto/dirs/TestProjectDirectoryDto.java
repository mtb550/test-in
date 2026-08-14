package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.DirectoryType;
import org.testin.mappers.markers.TestProjectMarker;


@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TestProjectDirectoryDto extends DirectoryDto {

    @NotNull
    @Builder.Default
    private TestCasesMainDirectoryDto testCasesDirectory = new TestCasesMainDirectoryDto();

    @NotNull
    @Builder.Default
    private TestRunsMainDirectoryDto testRunsDirectory = new TestRunsMainDirectoryDto();

    @NotNull
    private String pathName;

    @NotNull
    @Builder.Default
    private TestProjectMarker marker = new TestProjectMarker();




    @Override
    public @NotNull String getMarkerFileName() {
        return DirectoryType.TP.getMarker();
    }

    // The test project node is fixed: not renamed, moved, removed or pasted
    // into from the tree - it is managed through its own actions.

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
    public boolean isTransferTarget() {
        return false;
    }

    @Override
    public @NotNull DirectoryType getType() {
        return DirectoryType.TP;
    }
}
