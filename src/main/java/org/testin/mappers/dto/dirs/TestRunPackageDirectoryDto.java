package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.testin.enums.CreateNodeMenu;

@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TestRunPackageDirectoryDto extends DirectoryDto {
    @Override
    public @NonNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_RUN_PACKAGE;
    }

}
