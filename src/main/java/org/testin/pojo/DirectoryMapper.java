package org.testin.pojo;

import org.testin.pojo.dto.dirs.*;
import org.testin.util.notifications.Notifier;

import java.nio.file.Path;

public class DirectoryMapper {
    public static TestProjectDirectoryDto testProjectNode(final Path path) {
        try {
            TestCasesMainDirectoryDto tcd = new TestCasesMainDirectoryDto()
                    .setPath(path.resolve("testCases"))
                    .setName("Test Cases");

            TestRunsMainDirectoryDto trd = new TestRunsMainDirectoryDto()
                    .setPath(path.resolve("testRuns"))
                    .setName("Test Runs");

            return new TestProjectDirectoryDto()
                    .setTestCasesDirectory(tcd)
                    .setTestRunsDirectory(trd)
                    .setName(path.getFileName().toString())
                    //.setProjectStatus(ProjectStatus.valueOf(parts[1])) // todo, to be moved to .pr file with date created and created by and modified by. modifed at.
                    .setPath(path)
                    .setPathName(path.getFileName().toString());

        } catch (Exception e) {
            Notifier.error("Read Test Project Failed", "Skipping invalid format: " + path.getFileName().toString());
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static TestCasesMainDirectoryDto testCasesRootNode(Path path) {
        try {
            return new TestCasesMainDirectoryDto()
                    .setName(path.getFileName().toString())
                    .setPath(path);

        } catch (Exception e) {
            Notifier.error("Read Test Case Package Failed", "Failed to parse directory: " + path.getFileName());
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static TestRunsMainDirectoryDto testRunsRootNode(Path path) {
        try {
            return new TestRunsMainDirectoryDto()
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
