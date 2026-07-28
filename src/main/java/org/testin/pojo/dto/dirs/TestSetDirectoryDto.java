package org.testin.pojo.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.testin.pojo.CreateNodeMenu;
import org.testin.pojo.markers.TestSetMarker;

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
