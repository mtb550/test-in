package org.testin.editorPanel;

import com.intellij.testFramework.LightVirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;

@Getter
public class UnifiedVirtualFile extends LightVirtualFile {

    private final DirectoryDto dir;

    public UnifiedVirtualFile(final DirectoryDto dir, final FileType ft) {
        super(dir.getName());
        this.dir = dir;
        this.setFileType(ft);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public @NotNull String getUrl() {
        return "testin://" + dir.getPath().toAbsolutePath().toString().replace("\\", "/");
    }

    @Override
    public @NotNull String getPath() {
        return dir.getPath().toAbsolutePath().toString();
    }

    public TestSetDirectoryDto getTestSet() {
        return (TestSetDirectoryDto) dir;
    }

    public TestRunDirectoryDto getTestRun() {
        return (TestRunDirectoryDto) dir;
    }
}