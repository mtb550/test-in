package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.testin.enums.CreateNodeMenu;
import org.testin.mappers.markers.TestSetPackageMarker;

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
    public @NonNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_SET_PACKAGE;
    }

}
