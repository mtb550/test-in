package org.testin.model;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.DirectoryType;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.*;
import org.testin.model.markers.TestCasesMainDirectoryMarker;
import org.testin.model.markers.TestProjectMarker;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.markers.TestRunPackageMarker;
import org.testin.model.markers.TestRunsMainDirectoryMarker;
import org.testin.model.markers.TestSetMarker;
import org.testin.model.markers.TestSetPackageMarker;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Mapper;
import org.testin.util.Tools;

import java.io.File;
import java.nio.file.Path;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class DirectoryMapper {

    public @NotNull TestProjectDirectoryDto setTestProjectNode(final @NotNull Project p, final @NotNull Path path) {
        final String fileName = path.getFileName().toString();

        final TestProjectDirectoryDto tp = TestProjectDirectoryDto.builder()
                .name(fileName)
                .path(path)
                .pathName(fileName)
                .path2(Services.getInstance(p, Tools.class).buildPath2(null, fileName))
                .build();

        tp.setTestCasesDirectory(getTestCasesRootNode(p, path, tp));
        tp.setTestRunsDirectory(getTestRunsRootNode(p, path, tp));

        return tp;
    }

    public @NotNull TestProjectDirectoryDto getTestProjectNode(final @NotNull Project p, final @NotNull Path path) {
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

    public @NotNull TestCasesMainDirectoryDto getTestCasesRootNode(final @NotNull Project p, final @NotNull Path path, final @NotNull TestProjectDirectoryDto tp) {
        final Path dir = path.resolve(DirectoryType.TCD.getDisplayedName());
        return TestCasesMainDirectoryDto.builder()
                .path(dir)
                .name(DirectoryType.TCD.getDisplayedName())
                .parent(tp)
                .path2(Services.getInstance(p, Tools.class).buildPath2(tp.getPath2(), DirectoryType.TCD.getDisplayedName()))
                .marker(readMarkerSafe(Services.getInstance(p, Mapper.class),
                        dir.resolve(DirectoryType.TCD.getMarker()).toFile(),
                        TestCasesMainDirectoryMarker.class, "test cases directory", DirectoryType.TCD.getDisplayedName()))
                .build();
    }

    public @NotNull TestRunsMainDirectoryDto getTestRunsRootNode(final @NotNull Project p, final @NotNull Path path, final @NotNull TestProjectDirectoryDto tp) {
        final Path dir = path.resolve(DirectoryType.TRD.getDisplayedName());
        return TestRunsMainDirectoryDto.builder()
                .path(dir)
                .name(DirectoryType.TRD.getDisplayedName())
                .parent(tp)
                .path2(Services.getInstance(p, Tools.class).buildPath2(tp.getPath2(), DirectoryType.TRD.getDisplayedName()))
                .marker(readMarkerSafe(Services.getInstance(p, Mapper.class),
                        dir.resolve(DirectoryType.TRD.getMarker()).toFile(),
                        TestRunsMainDirectoryMarker.class, "test runs directory", DirectoryType.TRD.getDisplayedName()))
                .build();
    }

    public @NotNull TestSetPackageDirectoryDto getTestSetPackageNode(final @NotNull Project p, final @NotNull Path path, final @NotNull DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestSetPackageDirectoryDto testSetPackageDirectoryDto = TestSetPackageDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(p, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .marker(readMarkerSafe(Services.getInstance(p, Mapper.class),
                            path.resolve(DirectoryType.TSP.getMarker()).toFile(),
                            TestSetPackageMarker.class, "test set package", fileName))
                    .build();

            Logger.info("retrieve the test set package directory: " + testSetPackageDirectoryDto);
            return testSetPackageDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(p, Notifier.class).error(p, "Read Test Set Package Failed", "Failed to parse directory: " + fileName);
            Logger.error("readTestSetPackageNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public @NotNull TestRunPackageDirectoryDto getTestRunPackageNode(final @NotNull Project p, final @NotNull Path path, final @NotNull DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestRunPackageDirectoryDto testRunPackageDirectoryDto = TestRunPackageDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(p, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .marker(readMarkerSafe(Services.getInstance(p, Mapper.class),
                            path.resolve(DirectoryType.TRP.getMarker()).toFile(),
                            TestRunPackageMarker.class, "test run package", fileName))
                    .build();

            Logger.info("retrieve the test run package directory: " + testRunPackageDirectoryDto);
            return testRunPackageDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(p, Notifier.class).error(p, "Read Test Run Package Failed", "Failed to parse directory: " + fileName);
            Logger.error("readTestRunPackageNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public @NotNull TestSetDirectoryDto getTestSetNode(final @NotNull Project p, final @NotNull Path path, final @NotNull DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        try {
            TestSetDirectoryDto testSetDirectoryDto = TestSetDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(p, Tools.class).buildPath2(parent.getPath2(), fileName))
                    .marker(readMarkerSafe(Services.getInstance(p, Mapper.class),
                            path.resolve(DirectoryType.TS.getMarker()).toFile(),
                            TestSetMarker.class, "test set", fileName))
                    .build();

            Logger.info("retrieve the test set directory: " + testSetDirectoryDto);
            return testSetDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(p, Notifier.class).error(p, "Read Test Set Failed", "Failed to parse directory: " + fileName);
            Logger.error("readTestSetNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    /**
     * Builds a run node without reading the marker file (used when creating a new run).
     */
    public @NotNull TestRunDirectoryDto setTestRunNode(final @NotNull Project p, final @NotNull Path path, final @NotNull DirectoryDto parent) {
        return buildTestRunNode(p, path, parent, null);
    }

    /**
     * Builds a run node, reading its marker from disk (used when indexing an existing run).
     */
    public @NotNull TestRunDirectoryDto getTestRunNode(final @NotNull Project p, final @NotNull Path path, final @NotNull DirectoryDto parent) {
        final String fileName = path.getFileName().toString();
        final TestRunMarker marker = readMarkerSafe(Services.getInstance(p, Mapper.class),
                path.resolve(DirectoryType.TR.getMarker()).toFile(), TestRunMarker.class, "run", fileName);
        return buildTestRunNode(p, path, parent, marker);
    }

    private @NotNull TestRunDirectoryDto buildTestRunNode(final @NotNull Project p, final @NotNull Path path,
                                                          final @NotNull DirectoryDto parent, final @Nullable TestRunMarker marker) {
        final String fileName = path.getFileName().toString();
        try {
            final var builder = TestRunDirectoryDto
                    .builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(Services.getInstance(p, Tools.class).buildPath2(parent.getPath2(), fileName));

            if (marker != null) builder.marker(marker);

            final TestRunDirectoryDto testRunDirectoryDto = builder.build();
            Logger.info("retrieve the test run directory: " + testRunDirectoryDto);
            return testRunDirectoryDto;

        } catch (final Exception ex) {
            Services.getInstance(p, Notifier.class).error(p, "Read Test Run Failed", "Failed to parse directory: " + fileName);
            Logger.error("readTestRunNode: Failed to parse directory '" + fileName + "' at " + path.toAbsolutePath() + ": " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    private <M> @NotNull M readMarkerSafe(final @NotNull Mapper mapper, final @NotNull File file, final @NotNull Class<M> type, final @NotNull String kind, final @NotNull String name) {
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
