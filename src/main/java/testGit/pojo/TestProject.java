package testGit.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode(callSuper = true)
public class TestProject extends Directory {
    private ProjectStatus projectStatus;

    private TestCasesDirectory testCasesDirectory;

    private TestRunsDirectory testRunsDirectory;

    private String pathName;

    @Override
    public TestProject setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public TestProject setPath(Path path) {
        super.setPath(path);
        return this;
    }

}
