package org.testin.pojo.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.CreateNodeMenu;
import org.testin.pojo.markers.TestProjectMarker;

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
    public @NonNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_PROJECT;
    }

}
