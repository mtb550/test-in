package org.testin.pojo.dto.dirs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.testin.pojo.CreateNodeMenu;
import org.testin.pojo.TestRunStatus;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestRunDirectoryDto extends DirectoryDto {
    @JsonIgnore
    private final AtomicBoolean isLoadingStatus = new AtomicBoolean(false);

    @JsonIgnore
    private volatile TestRunStatus runStatus = null;

    @Override
    public TestRunDirectoryDto setPath(Path path) {
        super.setPath(path);
        return this;
    }

    @Override
    public TestRunDirectoryDto setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public CreateNodeMenu getMenu() {
        return CreateNodeMenu.TEST_RUN;
    }
}
