package testGit.pojo.mappers;

import testGit.pojo.TestRun;
import testGit.util.Notifier;

import java.nio.file.Path;

public class TestRunMapper {

    public static TestRun map(Path path) {
        try {
            return new TestRun()
                    .setName(path.getFileName().toString())
                    .setPath(path);

        } catch (Exception e) {
            Notifier.error("Read Test Run Failed", "Failed to parse directory: " + path.getFileName());
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }
}