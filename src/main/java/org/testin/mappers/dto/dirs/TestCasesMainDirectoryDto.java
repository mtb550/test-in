package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.testin.enums.CreateNodeMenu;

@Setter
@Getter
@NoArgsConstructor
@ToString(callSuper = true)
@SuperBuilder
public class TestCasesMainDirectoryDto extends DirectoryDto {
    @Override
    public @NonNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_CASES_MAIN_DIR;
    }

}
