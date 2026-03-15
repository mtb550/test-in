package testGit.editorPanel;

import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.DirectoryIcon;

import javax.swing.*;

public class FileType extends FakeFileType {

    public static final FileType TEST_RUN = new FileType(
            "Test Run",
            "Test Run Editor",
            DirectoryIcon.TR
    );

    public static final FileType TEST_CASE = new FileType(
            "Test Case",
            "Test Case Editor",
            DirectoryIcon.TS
    );

    private final String name;
    private final String description;
    private final DirectoryIcon icon;

    private FileType(String name, String description, DirectoryIcon icon) {
        this.name = name;
        this.description = description;
        this.icon = icon;
    }

    @Override
    public boolean isMyFileType(@NotNull VirtualFile file) {
        return false;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull String getDescription() {
        return description;
    }

    @Override
    public @Nullable Icon getIcon() {
        return icon.getValue();
    }
}