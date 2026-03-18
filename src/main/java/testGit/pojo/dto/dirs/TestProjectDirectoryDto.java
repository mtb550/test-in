package testGit.pojo.dto.dirs;

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
public class TestProjectDirectoryDto extends DirectoryDto {
    private ProjectStatus projectStatus;

    private TestCasesDirectoryDto testCasesDirectory;

    private TestRunsDirectoryDto testRunsDirectory;

    private String pathName;

    @Override
    public TestProjectDirectoryDto setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public TestProjectDirectoryDto setPath(Path path) {
        super.setPath(path);
        return this;
    }

}
