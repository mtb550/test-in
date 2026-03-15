package testGit.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.File;
import java.nio.file.Path;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(callSuper = true)
public class TestProject extends Directory {
    private ProjectStatus projectStatus;

    private TestPackage testCase;

    private TestPackage testRun;

    @Override
    public TestProject setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public TestProject setFilePath(Path filePath) {
        super.setFilePath(filePath);
        return this;
    }

    @Override
    public TestProject setFile(File file) {
        super.setFile(file);
        return this;
    }

    @Override
    public TestProject setFileName(String fileName) {
        super.setFileName(fileName);
        return this;
    }

    @Override
    public DirectoryIcon getIcon() {
        return super.getIcon();
    }

    @Override
    public TestProject setIcon(DirectoryIcon directoryIcon) {
        super.setIcon(directoryIcon);
        return this;
    }

    @Override
    public DirectoryType getType() {
        return super.getType();
    }

    @Override
    public TestProject setType(DirectoryType type) {
        super.setType(type);
        return this;
    }
}
