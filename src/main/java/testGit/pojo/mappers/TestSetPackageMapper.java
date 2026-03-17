package testGit.pojo.mappers;

import testGit.pojo.TestSetPackage;
import testGit.util.Notifier;

import java.nio.file.Path;

public class TestSetPackageMapper {
    public static TestSetPackage map(Path path) {
        try {
            return new TestSetPackage()
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