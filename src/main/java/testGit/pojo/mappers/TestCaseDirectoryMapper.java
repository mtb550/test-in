package testGit.pojo.mappers;

import testGit.pojo.TestCasesDirectory;
import testGit.util.Notifier;

import java.nio.file.Path;

public class TestCaseDirectoryMapper {
    public static TestCasesDirectory map(Path path) {
        try {
            return new TestCasesDirectory()
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