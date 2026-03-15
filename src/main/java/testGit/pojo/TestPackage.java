package testGit.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class TestPackage extends Directory {
    @Override
    public TestPackage setFilePath(Path filePath) {
        super.setFilePath(filePath);
        return this;
    }

    @Override
    public TestPackage setFile(File file) {
        super.setFile(file);
        return this;
    }

    @Override
    public TestPackage setFileName(String fileName) {
        super.setFileName(fileName);
        return this;
    }

    @Override
    public TestPackage setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public DirectoryIcon getIcon() {
        return super.getIcon();
    }

    @Override
    public TestPackage setIcon(DirectoryIcon directoryIcon) {
        super.setIcon(directoryIcon);
        return this;
    }

    @Override
    public DirectoryType getType() {
        return super.getType();
    }

    @Override
    public TestPackage setType(DirectoryType type) {
        super.setType(type);
        return this;
    }

}
