package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.testin.enums.CreateNodeMenu;
import org.testin.mappers.markers.TestSetMarker;

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
    public @NonNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_SET;
    }

}
