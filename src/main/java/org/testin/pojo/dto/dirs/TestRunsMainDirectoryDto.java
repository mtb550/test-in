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
public class TestRunsMainDirectoryDto extends DirectoryDto {
    @Override
    public TestRunsMainDirectoryDto setPath(Path path) {
        super.setPath(path);
        return this;
    }

    @Override
    public TestRunsMainDirectoryDto setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_RUNS_MAIN_DIR;
    }

}
