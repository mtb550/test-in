package testGit.pojo.tree.mappers;

import testGit.pojo.tree.dirs.TestSetPackageDirectory;
import testGit.util.Notifier;

import java.nio.file.Path;

public class TestSetPackageMapper {
    public static TestSetPackageDirectory map(Path path) {
        try {
            return new TestSetPackageDirectory()
                    .setName(path.getFileName().toString())
                    .setPath(path);

        } catch (Exception e) {
            Notifier.error("Read Test Case Package Failed", "Failed to parse directory: " + path.getFileName());
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }
}