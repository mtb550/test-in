package testGit.pojo.tree.mappers;

import testGit.pojo.ProjectStatus;
import testGit.pojo.tree.dirs.TestCasesDirectory;
import testGit.pojo.tree.dirs.TestProjectDirectory;
import testGit.pojo.tree.dirs.TestRunsDirectory;
import testGit.util.Notifier;

import java.nio.file.Path;

public class TestProjectMapper {

    public static TestProjectDirectory map(Path path) {
        try {
            String[] parts = path.getFileName().toString().split("_", 2);

            TestCasesDirectory tcd = new TestCasesDirectory()
                    .setPath(path.resolve("testCases"))
                    .setName("Test Cases");

            TestRunsDirectory trd = new TestRunsDirectory()
                    .setPath(path.resolve("testRuns"))
                    .setName("Test Runs");

            return new TestProjectDirectory()
                    .setTestCasesDirectory(tcd)
                    .setTestRunsDirectory(trd)
                    .setName(parts[0])
                    .setProjectStatus(ProjectStatus.valueOf(parts[1]))
                    .setPath(path)
                    .setPathName(path.getFileName().toString());

        } catch (Exception e) {
            Notifier.error("Read Test Project Failed", "Skipping invalid format: " + path.getFileName().toString());
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }
}