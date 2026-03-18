package testGit.pojo;

import testGit.pojo.dto.dirs.*;
import testGit.util.Notifier;

import java.nio.file.Path;

public class DirectoryMapper {
    public static TestProjectDirectoryDto testProjectNode(Path path) {
        try {
            String[] parts = path.getFileName().toString().split("_", 2);

            TestCasesDirectoryDto tcd = new TestCasesDirectoryDto()
                    .setPath(path.resolve("testCases"))
                    .setName("Test Cases");

            TestRunsDirectoryDto trd = new TestRunsDirectoryDto()
                    .setPath(path.resolve("testRuns"))
                    .setName("Test Runs");

            return new TestProjectDirectoryDto()
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

    public static TestCasesDirectoryDto testCasesRootNode(Path path) {
        try {
            return new TestCasesDirectoryDto()
                    .setName(path.getFileName().toString())
                    .setPath(path);

        } catch (Exception e) {
            Notifier.error("Read Test Case Package Failed", "Failed to parse directory: " + path.getFileName());
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static TestRunsDirectoryDto testRunsRootNode(Path path) {
        try {
            return new TestRunsDirectoryDto()
                    .setName(path.getFileName().toString())
                    .setPath(path);

        } catch (Exception e) {
            Notifier.error("Read Test Case Package Failed", "Failed to parse directory: " + path.getFileName());
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static TestSetPackageDirectoryDto testSetPackageNode(Path path) {
        try {
            return new TestSetPackageDirectoryDto()
                    .setName(path.getFileName().toString())
                    .setPath(path);

        } catch (Exception e) {
            Notifier.error("Read Test Case Package Failed", "Failed to parse directory: " + path.getFileName());
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static TestRunPackageDirectoryDto testRunPackageNode(Path path) {
        try {
            return new TestRunPackageDirectoryDto()
                    .setName(path.getFileName().toString())
                    .setPath(path);

        } catch (Exception e) {
            Notifier.error("Read Test Run Package Failed", "Failed to parse directory: " + path.getFileName());
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static TestSetDirectoryDto testSetNode(Path path) {
        try {
            return new TestSetDirectoryDto()
                    .setName(path.getFileName().toString())
                    .setPath(path);

        } catch (Exception e) {
            Notifier.error("Read Test Set Failed", "Failed to parse directory: " + path.getFileName());
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static TestRunDirectoryDto testRunNode(Path path) {
        try {
            return new TestRunDirectoryDto()
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
