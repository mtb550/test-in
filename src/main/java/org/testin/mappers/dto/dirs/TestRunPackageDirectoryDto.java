package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.testin.enums.CreateNodeMenu;
import org.testin.mappers.markers.TestRunPackageMarker;

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

}
