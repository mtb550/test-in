package org.testin.pojo.dto.dirs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.testin.pojo.CreateNodeMenu;

import java.nio.file.Path;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestSetPackageDirectoryDto extends DirectoryDto {
    @Override
    public TestSetPackageDirectoryDto setPath(Path path) {
        super.setPath(path);
        return this;
    }

    @Override
    public TestSetPackageDirectoryDto setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_SET_PACKAGE;
    }

}
