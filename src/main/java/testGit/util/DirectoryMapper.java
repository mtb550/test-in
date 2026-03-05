package testGit.util;

import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryStatus;
import testGit.pojo.DirectoryType;

import java.io.File;

public class DirectoryMapper {
    public static Directory map(@NotNull final File file) {
        try {
            String[] parts = file.getName().split("_", 3);

            return new Directory()
                    .setFile(file)
                    .setFilePath(file.toPath())
                    .setFileName(file.getName())
                    .setType(DirectoryType.valueOf(parts[0]))
                    .setName(parts[1])
                    .setStatus(DirectoryStatus.valueOf(parts[2]));
        } catch (Exception e) {
            Notifier.error("Read Directory Failed", "Skipping invalid directory format: " + file.getName());
            return null;
        }
    }
}
