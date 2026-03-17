package testGit.pojo.mappers;

import testGit.pojo.TestSet;
import testGit.util.Notifier;

import java.nio.file.Path;

public class TestSetMapper {
    public static TestSet map(Path path) {
        try {
            return new TestSet()
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