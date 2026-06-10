package org.testin.pojo.dto.dirs;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.jspecify.annotations.NonNull;
import org.testin.pojo.CreateNodeMenu;

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
