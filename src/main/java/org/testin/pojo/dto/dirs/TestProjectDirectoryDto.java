package org.testin.pojo.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.testin.pojo.CreateNodeMenu;
import org.testin.pojo.TestProjectMarker;

@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TestProjectDirectoryDto extends DirectoryDto {

    private TestCasesMainDirectoryDto testCasesDirectory;

    private TestRunsMainDirectoryDto testRunsDirectory;

    @NotNull
    private String pathName;

    private TestProjectMarker marker;

    @Override
    public @NonNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_PROJECT;
    }

}
