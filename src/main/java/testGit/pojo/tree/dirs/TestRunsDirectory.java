package testGit.pojo.tree.dirs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.nio.file.Path;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestRunsDirectory extends Directory {
    @Override
    public TestRunsDirectory setPath(Path path) {
        super.setPath(path);
        return this;
    }

    @Override
    public TestRunsDirectory setName(String name) {
        super.setName(name);
        return this;
    }

}
