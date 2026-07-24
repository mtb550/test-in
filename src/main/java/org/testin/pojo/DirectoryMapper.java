package org.testin.pojo;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.*;
import org.testin.pojo.markers.TestProjectMarker;
import org.testin.pojo.markers.TestRunMarker;
import org.testin.util.Mapper;
import org.testin.util.Tools;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import java.nio.file.Path;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class DirectoryMapper {

    public @NotNull TestProjectDirectoryDto setTestProjectNode(final @NotNull Project project, final Path path) {
        final String fileName = path.getFileName().toString();

        final TestProjectDirectoryDto tp = TestProjectDirectoryDto.builder()
                .name(fileName)
                .path(path)
                .pathName(fileName)
                .path2(Services.getInstance(project, Tools.class).buildPath2(null, fileName))
                .build();

        final TestCasesMainDirectoryDto tcd = setTestCasesRootNode(project, path, tp);
        final TestRunsMainDirectoryDto trd = setTestRunsRootNode(project, path, tp);

        tp.setTestCasesDirectory(tcd);
        tp.setTestRunsDirectory(trd);

        return tp;
    }

    public @NotNull TestProjectDirectoryDto getTestProjectNode(final @NotNull Project project, final Path path) {
        final String fileName = path.getFileName().toString();
        try {
            final Mapper mapper = Services.getInstance(project, Mapper.class);
            final TestProjectMarker marker = mapper.readValue(path.resolve(DirectoryType.TP.getMarker()).toFile(), TestProjectMarker.class);
            if (marker == null) throw new IllegalArgumentException("Marker not found");

            final TestProjectDirectoryDto tp = TestProjectDirectoryDto.builder()
                    .name(fileName)
                    .path(path)
                    .pathName(fileName)
                    .path2(Services.getInstance(project, Tools.class).buildPath2(null, fileName))
                    .marker(marker)
                    .build();

            final @NotNull TestCasesMainDirectoryDto tcd = getTestCasesRootNode(project, tp.getPath(), tp);
            final @NotNull TestRunsMainDirectoryDto trd = getTestRunsRootNode(project, path, tp);

            tp.setTestCasesDirectory(tcd);
            tp.setTestRunsDirectory(trd);

            Log.info("retrieve the project directory: " + tp);
            return tp;

        } catch (final Exception ex) {
            Services.getInstance(project, Notifier.class).error(project, "Read Test Project Failed", "Skipping invalid format: " + fileName);
            Log.error("readTestProjectNode: Failed to parse project '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public @NotNull TestCasesMainDirectoryDto setTestCasesRootNode(final @NotNull Project project, final Path path, final TestProjectDirectoryDto tp) {
        return TestCasesMainDirectoryDto.builder()
                .path(path.resolve(DirectoryType.TCD.getDisplayedName()))
                .name(DirectoryType.TCD.getDisplayedName())
                .parent(tp)
                .path2(Services.getInstance(project, Tools.class).buildPath2(tp.getPath2(), DirectoryType.TCD.getDisplayedName()))
                .build();
    }

    public @NotNull TestCasesMainDirectoryDto getTestCasesRootNode(final @NotNull Project project, final Path path, final TestProjectDirectoryDto tp) {
        return TestCasesMainDirectoryDto.builder()
                .path(path.resolve(DirectoryType.TCD.getDisplayedName()))
                .name(DirectoryType.TCD.getDisplayedName())
                .parent(tp)
                .path2(Services.getInstance(project, Tools.class).buildPath2(tp.getPath2(), DirectoryType.TCD.getDisplayedName()))
                .build();
    }

    public @NotNull TestRunsMainDirectoryDto setTestRunsRootNode(final @NotNull Project project, final Path path, final TestProjectDirectoryDto tp) {
        return TestRunsMainDirectoryDto.builder()
                .path(path.resolve(DirectoryType.TRD.getDisplayedName()))
                .name(DirectoryType.TRD.getDisplayedName())
                .parent(tp)
                .path2(Services.getInstance(project, Tools.class).buildPath2(tp.getPath2(), DirectoryType.TRD.getDisplayedName()))
                .build();
    }

    public @NotNull TestRunsMainDirectoryDto getTestRunsRootNode(final @NotNull Project project, final Path path, final TestProjectDirectoryDto tp) {
        return TestRunsMainDirectoryDto.builder()
                .path(path.resolve(DirectoryType.TRD.getDisplayedName()))
                .name(DirectoryType.TRD.getDisplayedName())
                .parent(tp)
                .path2(Services.getInstance(project, Tools.class).buildPath2(tp.getPath2(), DirectoryType.TRD.getDisplayedName()))
                .build();
    }

    public @NotNull TestSetPackageDirectoryDto getTestSetPackageNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestSetPackageDirectoryDto testSetPackageDirectoryDto = TestSetPackageDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(project, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .build();

            Log.info("retrieve the test set package directory: " + testSetPackageDirectoryDto);
            return testSetPackageDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(project, Notifier.class).error(project, "Read Test Set Package Failed", "Failed to parse directory: " + fileName);
            Log.error("readTestSetPackageNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public @NotNull TestRunPackageDirectoryDto getTestRunPackageNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestRunPackageDirectoryDto testRunPackageDirectoryDto = TestRunPackageDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(project, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .build();

            Log.info("retrieve the test run package directory: " + testRunPackageDirectoryDto);
            return testRunPackageDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(project, Notifier.class).error(project, "Read Test Run Package Failed", "Failed to parse directory: " + fileName);
            Log.error("readTestRunPackageNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public @NotNull TestSetDirectoryDto setTestSetNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            Log.info("retrieve the test set directory: " + fileName);

            TestSetDirectoryDto testSetDirectoryDto = TestSetDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(project, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .build();

            Log.info("retrieve the test set directory: " + testSetDirectoryDto);
            return testSetDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(project, Notifier.class).error(project, "Read Test Set Failed", "Failed to parse directory: " + fileName);
            Log.error("readTestSetNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public @NotNull TestSetDirectoryDto getTestSetNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            Log.info("retrieve the test set directory: " + fileName);
            TestSetDirectoryDto testSetDirectoryDto = TestSetDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(project, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .build();

            Log.info("retrieve the test set directory: " + testSetDirectoryDto);
            return testSetDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(project, Notifier.class).error(project, "Read Test Set Failed", "Failed to parse directory: " + fileName);
            Log.error("readTestSetNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public @NotNull TestRunDirectoryDto setTestRunNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            final Path markerPath = path.resolve(DirectoryType.TR.getMarker());
            final TestRunMarker marker = Services.getInstance(project, Mapper.class).readValue(markerPath.toFile(), TestRunMarker.class);

            TestRunDirectoryDto testRunDirectoryDto = TestRunDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(project, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .marker(marker)
                    .build();

            Log.info("retrieve the test run directory: " + testRunDirectoryDto);
            return testRunDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(project, Notifier.class).error(project, "Read Test Run Failed", "Failed to parse directory: " + fileName);
            Log.error("readTestRunNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public @NotNull TestRunDirectoryDto getTestRunNode(final @NotNull Project project, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            final Path markerPath = path.resolve(DirectoryType.TR.getMarker());
            final TestRunMarker marker = Services.getInstance(project, Mapper.class).readValue(markerPath.toFile(), TestRunMarker.class);


            TestRunDirectoryDto testRunDirectoryDto = TestRunDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(project, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .marker(marker)
                    .build();

            Log.info("retrieve the test run directory: " + testRunDirectoryDto);
            return testRunDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(project, Notifier.class).error(project, "Read Test Run Failed", "Failed to parse directory: " + fileName);
            Log.error("readTestRunNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}
