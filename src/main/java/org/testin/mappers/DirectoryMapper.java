package org.testin.mappers;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.DirectoryType;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.*;
import org.testin.mappers.markers.TestProjectMarker;
import org.testin.mappers.markers.TestRunMarker;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Mapper;
import org.testin.util.Tools;

import java.io.File;
import java.nio.file.Path;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class DirectoryMapper {

    public @NotNull TestProjectDirectoryDto setTestProjectNode(final @NotNull Project p, final Path path) {
        final String fileName = path.getFileName().toString();

        final TestProjectDirectoryDto tp = TestProjectDirectoryDto.builder()
                .name(fileName)
                .path(path)
                .pathName(fileName)
                .path2(Services.getInstance(p, Tools.class).buildPath2(null, fileName))
                .build();

        final TestCasesMainDirectoryDto tcd = setTestCasesRootNode(p, path, tp);
        final TestRunsMainDirectoryDto trd = setTestRunsRootNode(p, path, tp);

        tp.setTestCasesDirectory(tcd);
        tp.setTestRunsDirectory(trd);

        return tp;
    }

    public @NotNull TestProjectDirectoryDto getTestProjectNode(final @NotNull Project p, final Path path) {
        final String fileName = path.getFileName().toString();
        try {
            final Mapper mapper = Services.getInstance(p, Mapper.class);
            final TestProjectMarker marker = readMarkerSafe(mapper,
                    path.resolve(DirectoryType.TP.getMarker()).toFile(),
                    TestProjectMarker.class, "project", fileName);

            final TestProjectDirectoryDto tp = TestProjectDirectoryDto.builder()
                    .name(fileName)
                    .path(path)
                    .pathName(fileName)
                    .path2(Services.getInstance(p, Tools.class).buildPath2(null, fileName))
                    .marker(marker)
                    .build();

            final @NotNull TestCasesMainDirectoryDto tcd = getTestCasesRootNode(p, tp.getPath(), tp);
            final @NotNull TestRunsMainDirectoryDto trd = getTestRunsRootNode(p, path, tp);

            tp.setTestCasesDirectory(tcd);
            tp.setTestRunsDirectory(trd);

            Logger.info("retrieve the project directory: " + tp);
            return tp;

        } catch (final Exception ex) {
            Services.getInstance(p, Notifier.class).error(p, "Read Test Project Failed", "Skipping invalid format: " + fileName);
            Logger.error("readTestProjectNode: Failed to parse project '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public @NotNull TestCasesMainDirectoryDto setTestCasesRootNode(final @NotNull Project p, final Path path, final TestProjectDirectoryDto tp) {
        return TestCasesMainDirectoryDto.builder()
                .path(path.resolve(DirectoryType.TCD.getDisplayedName()))
                .name(DirectoryType.TCD.getDisplayedName())
                .parent(tp)
                .path2(Services.getInstance(p, Tools.class).buildPath2(tp.getPath2(), DirectoryType.TCD.getDisplayedName()))
                .build();
    }

    public @NotNull TestCasesMainDirectoryDto getTestCasesRootNode(final @NotNull Project p, final Path path, final TestProjectDirectoryDto tp) {
        return TestCasesMainDirectoryDto.builder()
                .path(path.resolve(DirectoryType.TCD.getDisplayedName()))
                .name(DirectoryType.TCD.getDisplayedName())
                .parent(tp)
                .path2(Services.getInstance(p, Tools.class).buildPath2(tp.getPath2(), DirectoryType.TCD.getDisplayedName()))
                .build();
    }

    public @NotNull TestRunsMainDirectoryDto setTestRunsRootNode(final @NotNull Project p, final Path path, final TestProjectDirectoryDto tp) {
        return TestRunsMainDirectoryDto.builder()
                .path(path.resolve(DirectoryType.TRD.getDisplayedName()))
                .name(DirectoryType.TRD.getDisplayedName())
                .parent(tp)
                .path2(Services.getInstance(p, Tools.class).buildPath2(tp.getPath2(), DirectoryType.TRD.getDisplayedName()))
                .build();
    }

    public @NotNull TestRunsMainDirectoryDto getTestRunsRootNode(final @NotNull Project p, final Path path, final TestProjectDirectoryDto tp) {
        return TestRunsMainDirectoryDto.builder()
                .path(path.resolve(DirectoryType.TRD.getDisplayedName()))
                .name(DirectoryType.TRD.getDisplayedName())
                .parent(tp)
                .path2(Services.getInstance(p, Tools.class).buildPath2(tp.getPath2(), DirectoryType.TRD.getDisplayedName()))
                .build();
    }

    public @NotNull TestSetPackageDirectoryDto getTestSetPackageNode(final @NotNull Project p, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestSetPackageDirectoryDto testSetPackageDirectoryDto = TestSetPackageDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(p, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .build();

            Logger.info("retrieve the test set package directory: " + testSetPackageDirectoryDto);
            return testSetPackageDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(p, Notifier.class).error(p, "Read Test Set Package Failed", "Failed to parse directory: " + fileName);
            Logger.error("readTestSetPackageNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public @NotNull TestRunPackageDirectoryDto getTestRunPackageNode(final @NotNull Project p, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestRunPackageDirectoryDto testRunPackageDirectoryDto = TestRunPackageDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(p, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .build();

            Logger.info("retrieve the test run package directory: " + testRunPackageDirectoryDto);
            return testRunPackageDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(p, Notifier.class).error(p, "Read Test Run Package Failed", "Failed to parse directory: " + fileName);
            Logger.error("readTestRunPackageNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public @NotNull TestSetDirectoryDto setTestSetNode(final @NotNull Project p, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            Logger.info("retrieve the test set directory: " + fileName);

            TestSetDirectoryDto testSetDirectoryDto = TestSetDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(p, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .build();

            Logger.info("retrieve the test set directory: " + testSetDirectoryDto);
            return testSetDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(p, Notifier.class).error(p, "Read Test Set Failed", "Failed to parse directory: " + fileName);
            Logger.error("readTestSetNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public @NotNull TestSetDirectoryDto getTestSetNode(final @NotNull Project p, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            Logger.info("retrieve the test set directory: " + fileName);
            TestSetDirectoryDto testSetDirectoryDto = TestSetDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(p, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .build();

            Logger.info("retrieve the test set directory: " + testSetDirectoryDto);
            return testSetDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(p, Notifier.class).error(p, "Read Test Set Failed", "Failed to parse directory: " + fileName);
            Logger.error("readTestSetNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public @NotNull TestRunDirectoryDto setTestRunNode(final @NotNull Project p, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {

            TestRunDirectoryDto testRunDirectoryDto = TestRunDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(p, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .build();

            Logger.info("retrieve the test run directory: " + testRunDirectoryDto);
            return testRunDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(p, Notifier.class).error(p, "Read Test Run Failed", "Failed to parse directory: " + fileName);
            Logger.error("readTestRunNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public @NotNull TestRunDirectoryDto getTestRunNode(final @NotNull Project p, final Path path, final DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            final Path markerPath = path.resolve(DirectoryType.TR.getMarker());
            final TestRunMarker marker = readMarkerSafe(Services.getInstance(p, Mapper.class),
                    markerPath.toFile(), TestRunMarker.class, "run", fileName);


            TestRunDirectoryDto testRunDirectoryDto = TestRunDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(p, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .marker(marker)
                    .build();

            Logger.info("retrieve the test run directory: " + testRunDirectoryDto);
            return testRunDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(p, Notifier.class).error(p, "Read Test Run Failed", "Failed to parse directory: " + fileName);
            Logger.error("readTestRunNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    /**
     * Reads a marker file, falling back to a default marker when the file is
     * missing or empty/corrupt, so a single bad marker can never kill the scan
     * of a whole project or run. Markers are optional metadata.
     */
    private <M> M readMarkerSafe(final @NotNull Mapper mapper, final @NotNull File file, final @NotNull Class<M> type, final String kind, final String name) {
        try {
            return mapper.readValue(file, type);
        } catch (final Exception ex) {
            Logger.warn("Missing/empty " + kind + " marker '" + name + "', using defaults: " + ex.getMessage());
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (final Exception fallbackEx) {
                throw new RuntimeException("Cannot create default " + kind + " marker", fallbackEx);
            }
        }
    }
}
