package testGit.pojo.tree.dirs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import testGit.pojo.ProjectStatus;

import java.nio.file.Path;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(callSuper = true)
public class TestProjectDirectory extends Directory {
    private ProjectStatus projectStatus;

    private TestCasesDirectory testCasesDirectory;

    private TestRunsDirectory testRunsDirectory;

    private String pathName;

    @Override
    public TestProjectDirectory setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public TestProjectDirectory setPath(Path path) {
        super.setPath(path);
        return this;
    }

}
