package org.testin.pojo;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.*;
import org.testin.pojo.markers.TestProjectMarker;
import org.testin.util.Mapper;
import org.testin.util.Tools;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import java.nio.file.Path;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class DirectoryMapper {
    public TestProjectDirectoryDto readTestProjectNode(final @NotNull Project project, final Path path) {
        final String fileName = path.getFileName().toString();
        try {
            final TestProjectMarker marker = Services.getInstance(project, Mapper.class).readValue(path.resolve(DirectoryType.TP.getMarker()).toFile(), TestProjectMarker.class);

            final TestProjectDirectoryDto tp = TestProjectDirectoryDto.builder()
                    .name(fileName)
                    .path(path)
                    .pathName(fileName)
                    .fqcn(List.of(Services.getInstance(project, Tools.class).sanitizePackageName(fileName)))
                    .path2(Services.getInstance(project, Tools.class).buildPath2(null, fileName))
                    .marker(marker)
                    .build();

            TestCasesMainDirectoryDto tcd = TestCasesMainDirectoryDto.builder()
                    .path(path.resolve(DirectoryType.TCD.getDisplayedName()))
                    .name(DirectoryType.TCD.getDisplayedName())
                    .fqcn(tp.getFqcn())
                    .parent(tp)
                    .path2(Services.getInstance(project, Tools.class).buildPath2(tp.getPath2(), DirectoryType.TCD.getDisplayedName()))
                    .build();

            TestRunsMainDirectoryDto trd = TestRunsMainDirectoryDto.builder()
                    .path(path.resolve(DirectoryType.TRD.getDisplayedName()))
                    .name(DirectoryType.TRD.getDisplayedName())
                    .fqcn(tp.getFqcn())
                    .parent(tp)
                    .path2(Services.getInstance(project, Tools.class).buildPath2(tp.getPath2(), DirectoryType.TRD.getDisplayedName()))
                    .build();

            tp.setTestCasesDirectory(tcd);
            tp.setTestRunsDirectory(trd);

            Log.info("retrieve the project directory: " + tp);
            return tp;

        } catch (Exception e) {
            Services.getInstance(project, Notifier.class).error(project, "Read Test Project Failed", "Skipping invalid format: " + fileName);
            Log.error(e.getMessage());
            Log.error("Exception: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public TestCasesMainDirectoryDto readTestCasesRootNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestCasesMainDirectoryDto tcd = TestCasesMainDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn(parent.getFqcn())
                    .path2(Services.getInstance(project, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .build();

            Log.info("retrieve the test cases main directory: " + tcd);
            return tcd;

        } catch (Exception e) {
            Services.getInstance(project, Notifier.class).error(project, "Read Test Case Package Failed", "Failed to parse directory: " + path.getFileName());
            Log.error(e.getMessage());
            Log.error("Exception: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public TestRunsMainDirectoryDto readTestRunsRootNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestRunsMainDirectoryDto trd = TestRunsMainDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn((parent.getFqcn()))
                    .path2(Services.getInstance(project, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .build();

            Log.info("retrieve the test runs main directory: " + trd);
            return trd;

        } catch (Exception e) {
            Services.getInstance(project, Notifier.class).error(project, "Read Test Case Package Failed", "Failed to parse directory: " + path.getFileName());
            Log.error(e.getMessage());
            Log.error("Exception: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public TestSetPackageDirectoryDto readTestSetPackageNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestSetPackageDirectoryDto testSetPackageDirectoryDto = TestSetPackageDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn(Services.getInstance(project, Tools.class).appendFqcn(parent.getFqcn(), fileName, DirectoryType.TSP))
                    .path2(Services.getInstance(project, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .build();

            Log.info("retrieve the test set package directory: " + testSetPackageDirectoryDto);
            return testSetPackageDirectoryDto;

        } catch (Exception e) {
            Services.getInstance(project, Notifier.class).error(project, "Read Test Case Package Failed", "Failed to parse directory: " + path.getFileName());
            Log.error(e.getMessage());
            Log.error("Exception: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public TestRunPackageDirectoryDto readTestRunPackageNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestRunPackageDirectoryDto testRunPackageDirectoryDto = TestRunPackageDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn(Services.getInstance(project, Tools.class).appendFqcn(parent.getFqcn(), fileName, DirectoryType.TRP))
                    .path2(Services.getInstance(project, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .build();

            Log.info("retrieve the test run package directory: " + testRunPackageDirectoryDto);
            return testRunPackageDirectoryDto;

        } catch (Exception e) {
            Services.getInstance(project, Notifier.class).error(project, "Read Test Run Package Failed", "Failed to parse directory: " + path.getFileName());
            Log.error(e.getMessage());
            Log.error("Exception: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public TestSetDirectoryDto readTestSetNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            Log.info("retrieve the test set directory: " + fileName);
            TestSetDirectoryDto testSetDirectoryDto = TestSetDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn(Services.getInstance(project, Tools.class).appendFqcn(parent.getFqcn(), fileName, DirectoryType.TS))
                    .path2(Services.getInstance(project, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .build();

            Log.info("retrieve the test set directory: " + testSetDirectoryDto);
            return testSetDirectoryDto;

        } catch (Exception e) {
            Services.getInstance(project, Notifier.class).error(project, "Read Test Set Failed", "Failed to parse directory: " + path.getFileName());
            Log.error(e.getMessage());
            Log.error("Exception: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    public TestRunDirectoryDto readTestRunNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            final TestRunMarker marker = Services.getInstance(project, Mapper.class).readValue(path.resolve(DirectoryType.TR.getMarker()).toFile(), TestRunMarker.class);

            TestRunDirectoryDto testRunDirectoryDto = TestRunDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn(Services.getInstance(project, Tools.class).appendFqcn(parent.getFqcn(), fileName, DirectoryType.TR))
                    .path2(Services.getInstance(project, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .marker(marker)
                    .build();

            Log.info("retrieve the test run directory: " + testRunDirectoryDto);
            return testRunDirectoryDto;

        } catch (Exception e) {
            Services.getInstance(project, Notifier.class).error(project, "Read Test Run Failed", "Failed to parse directory: " + path.getFileName());
            Log.error(e.getMessage());
            Log.error("Exception: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }
}
