package testGit.pojo.tree.mappers;

import testGit.pojo.tree.dirs.TestSetDirectory;
import testGit.util.Notifier;

import java.nio.file.Path;

public class TestSetMapper {
    public static TestSetDirectory map(Path path) {
        try {
            return new TestSetDirectory()
                    .setName(path.getFileName().toString())
                    .setPath(path);

        } catch (Exception e) {
            Notifier.error("Read Test Set Failed", "Failed to parse directory: " + path.getFileName());
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }
}