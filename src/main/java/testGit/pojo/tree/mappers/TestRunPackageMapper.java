package testGit.pojo.tree.mappers;

import testGit.pojo.tree.dirs.TestRunPackageDirectory;
import testGit.util.Notifier;

import java.nio.file.Path;

public class TestRunPackageMapper {
    public static TestRunPackageDirectory map(Path path) {
        try {
            return new TestRunPackageDirectory()
                    .setName(path.getFileName().toString())
                    .setPath(path);

        } catch (Exception e) {
            Notifier.error("Read Test Run Package Failed", "Failed to parse directory: " + path.getFileName());
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }
}