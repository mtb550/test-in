package org.testin.pojo;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.*;
import org.testin.util.Mapper;
import org.testin.util.Tools;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;

import java.nio.file.Path;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DirectoryMapper {

    private static final DirectoryMapper INSTANCE = new DirectoryMapper();

    public static DirectoryMapper getInstance() {
        return INSTANCE;
    }

    public TestProjectDirectoryDto testProjectNode(final @NotNull Project project, final Path path) { // todo, path is Testin path , no need to pass the path here.
        final String fileName = path.getFileName().toString();
        try {
            final TestProjectDirectoryDto tp = TestProjectDirectoryDto.builder()
                    .name(fileName)
                    .path(path)
                    .pathName(fileName)
                    .fqcn(List.of(Tools.getInstance().sanitizePackageName(fileName)))
                    .path2(Tools.getInstance().buildPath2(null, fileName))
                    .marker(Mapper.readValue(path.resolve(DirectoryType.TP.getMarker()).toFile(), TestProjectMarker.class))
                    .build();

            // todo, to be removed. call testCasesRootNode instead.
            TestCasesMainDirectoryDto tcd = TestCasesMainDirectoryDto.builder()
                    .path(path.resolve(DirectoryType.TCD.getDisplayedName()))
                    .name(DirectoryType.TCD.getDisplayedName())
                    .fqcn(tp.getFqcn())
                    .parent(tp)
                    .path2(Tools.getInstance().buildPath2(tp.getPath2(), DirectoryType.TCD.getDisplayedName()))
                    .build();

            // todo, to be removed. call testRunsRootNode instead.
            TestRunsMainDirectoryDto trd = TestRunsMainDirectoryDto.builder()
                    .path(path.resolve(DirectoryType.TRD.getDisplayedName()))
                    .name(DirectoryType.TRD.getDisplayedName())
                    .fqcn(tp.getFqcn())
                    .parent(tp)
                    .path2(Tools.getInstance().buildPath2(tp.getPath2(), DirectoryType.TRD.getDisplayedName()))
                    .build();

            tp.setTestCasesDirectory(tcd);
            tp.setTestRunsDirectory(trd);

            Log.info("retrieve the project directory: " + tp);
            return tp;

        } catch (Exception e) {
            Notifier.getInstance().error(project, "Read Test Project Failed", "Skipping invalid format: " + fileName);
            Log.error(e.getMessage());
            Log.error("Exception: " + e.getMessage());
            return null;
        }
    }

    public TestCasesMainDirectoryDto testCasesRootNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestCasesMainDirectoryDto tcd = TestCasesMainDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn(parent.getFqcn())
                    .path2(Tools.getInstance().buildPath2(parent.getPath2(), fileName))
                    .build();

            Log.info("retrieve the test cases main directory: " + tcd);
            return tcd;

        } catch (Exception e) {
            Notifier.getInstance().error(project, "Read Test Case Package Failed", "Failed to parse directory: " + path.getFileName());
            Log.error(e.getMessage());
            Log.error("Exception: " + e.getMessage());
            return null;
        }
    }

    public TestRunsMainDirectoryDto testRunsRootNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestRunsMainDirectoryDto trd = TestRunsMainDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn((parent.getFqcn()))
                    .path2(Tools.getInstance().buildPath2(parent.getPath2(), fileName))
                    .build();

            Log.info("retrieve the test runs main directory: " + trd);
            return trd;

        } catch (Exception e) {
            Notifier.getInstance().error(project, "Read Test Case Package Failed", "Failed to parse directory: " + path.getFileName());
            Log.error(e.getMessage());
            Log.error("Exception: " + e.getMessage());
            return null;
        }
    }

    public TestSetPackageDirectoryDto testSetPackageNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestSetPackageDirectoryDto testSetPackageDirectoryDto = TestSetPackageDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn(Tools.getInstance().appendFqcn(parent.getFqcn(), fileName, DirectoryType.TSP))
                    .path2(Tools.getInstance().buildPath2(parent.getPath2(), fileName))
                    .build();

            Log.info("retrieve the test set package directory: " + testSetPackageDirectoryDto);
            return testSetPackageDirectoryDto;

        } catch (Exception e) {
            Notifier.getInstance().error(project, "Read Test Case Package Failed", "Failed to parse directory: " + path.getFileName());
            Log.error(e.getMessage());
            Log.error("Exception: " + e.getMessage());
            return null;
        }
    }

    public TestRunPackageDirectoryDto testRunPackageNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestRunPackageDirectoryDto testRunPackageDirectoryDto = TestRunPackageDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn(Tools.getInstance().appendFqcn(parent.getFqcn(), fileName, DirectoryType.TRP))
                    .path2(Tools.getInstance().buildPath2(parent.getPath2(), fileName))
                    .build();

            Log.info("retrieve the test run package directory: " + testRunPackageDirectoryDto);
            return testRunPackageDirectoryDto;

        } catch (Exception e) {
            Notifier.getInstance().error(project, "Read Test Run Package Failed", "Failed to parse directory: " + path.getFileName());
            Log.error(e.getMessage());
            Log.error("Exception: " + e.getMessage());
            return null;
        }
    }

    public TestSetDirectoryDto testSetNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            Log.info("retrieve the test set directory: " + fileName);
            TestSetDirectoryDto testSetDirectoryDto = TestSetDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn(Tools.getInstance().appendFqcn(parent.getFqcn(), fileName, DirectoryType.TS))
                    .path2(Tools.getInstance().buildPath2(parent.getPath2(), fileName))
                    .build();

            Log.info("retrieve the test set directory: " + testSetDirectoryDto);
            return testSetDirectoryDto;

        } catch (Exception e) {
            Notifier.getInstance().error(project, "Read Test Set Failed", "Failed to parse directory: " + path.getFileName());
            Log.error(e.getMessage());
            Log.error("Exception: " + e.getMessage());
            return null;
        }
    }

    public TestRunDirectoryDto testRunNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestRunDirectoryDto testRunDirectoryDto = TestRunDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn(Tools.getInstance().appendFqcn(parent.getFqcn(), fileName, DirectoryType.TR))
                    .path2(Tools.getInstance().buildPath2(parent.getPath2(), fileName))
                    .build();

            Log.info("retrieve the test run directory: " + testRunDirectoryDto);
            return testRunDirectoryDto;

        } catch (Exception e) {
            Notifier.getInstance().error(project, "Read Test Run Failed", "Failed to parse directory: " + path.getFileName());
            Log.error(e.getMessage());
            Log.error("Exception: " + e.getMessage());
            return null;
        }
    }
}