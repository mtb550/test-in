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
@SuperBuilder
@ToString(callSuper = true)
public class TestSetPackageDirectoryDto extends DirectoryDto {
    @Override
    public @NonNull CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_SET_PACKAGE;
    }

}
