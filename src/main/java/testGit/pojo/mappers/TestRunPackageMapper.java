package testGit.pojo.mappers;

import testGit.pojo.TestRunPackage;
import testGit.util.Notifier;

import java.nio.file.Path;

public class TestRunPackageMapper {
    public static TestRunPackage map(Path path) {
        try {
            return new TestRunPackage()
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