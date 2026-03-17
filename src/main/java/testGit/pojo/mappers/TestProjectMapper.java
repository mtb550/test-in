package testGit.pojo.mappers;

import testGit.pojo.ProjectStatus;
import testGit.pojo.TestCasesDirectory;
import testGit.pojo.TestProject;
import testGit.pojo.TestRunsDirectory;
import testGit.util.Notifier;

import java.nio.file.Path;

public class TestProjectMapper {

    public static TestProject map(Path path) {
        try {
            String[] parts = path.getFileName().toString().split("_", 2);

            TestCasesDirectory tcd = new TestCasesDirectory()
                    .setPath(path.resolve("testCases"))
                    .setName("Test Cases");

            TestRunsDirectory trd = new TestRunsDirectory()
                    .setPath(path.resolve("testRuns"))
                    .setName("Test Runs");

            return new TestProject()
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