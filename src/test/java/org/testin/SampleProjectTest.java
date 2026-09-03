package org.testin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.markers.TestProjectMarker;
import org.testin.model.markers.TestRunMarker;
import org.testin.model.markers.TestSetMarker;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The committed sample project parses, and keeps parsing (#107).
 * <p>
 * {@code samples/} is documentation as much as data: it is the only committed
 * example of the seven marker formats, and the tree {@code runIde} opens onto.
 * Documentation that nothing reads goes stale, and stale sample data is worse
 * than none - a developer trusts it, and it describes a format the plugin
 * stopped writing two releases ago.
 * <p>
 * So the sample is read here through the same model the plugin reads it with. A
 * field renamed, a marker gaining a value, an enum constant deleted: any of them
 * stops this passing and names the file to fix, at the moment the change is made
 * rather than the next time somebody opens the sandbox.
 * <p>
 * Deliberately not a platform test. This repository has none, and #107 asked for
 * one - writing the first harness is a larger job than the fixture it would
 * check, and every assertion below holds without it.
 */
public class SampleProjectTest {

    /**
     * The sample, found from the module rather than from the working directory.
     * Gradle runs tests with the module as the working directory, but a run
     * started from the IDE need not, so the repository root is walked up to.
     */
    private static @NotNull Path samples() {
        @NotNull Path here = Path.of("").toAbsolutePath();

        while (!Files.isDirectory(here.resolve("samples"))) {
            here = here.getParent();
            if (here == null) throw new IllegalStateException("No samples/ directory above " + Path.of("").toAbsolutePath());
        }

        return here.resolve("samples");
    }

    private static @NotNull Path demo() {
        return samples().resolve("testin-root").resolve("Demo");
    }

    /**
     * The plugin's own mapper settings, so this reads the files the way the
     * indexer does rather than the way a default Jackson would.
     */
    private static @NotNull ObjectMapper mapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .setTimeZone(TimeZone.getDefault());
    }

    private static <T> @NotNull T read(final @NotNull Path file, final @NotNull Class<T> type) {
        try {
            return mapper().readValue(file.toFile(), type);
        } catch (final IOException ex) {
            throw new AssertionError("The sample file " + file + " no longer parses as " + type.getSimpleName()
                    + ". Either the format changed and samples/ has to follow, or the sample is wrong: " + ex.getMessage(), ex);
        }
    }

    private static @NotNull List<Path> filesNamed(final @NotNull Path root, final @NotNull String name) {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(path -> path.getFileName().toString().equals(name)).toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not walk the sample at " + root + ": " + ex.getMessage(), ex);
        }
    }

    @Test
    public void everyMarkerFormatHasACommittedExample() {
        final @NotNull List<String> missing = new ArrayList<>();

        for (final DirectoryType type : DirectoryType.values()) {
            if (filesNamed(demo(), type.getMarker()).isEmpty()) missing.add(type.getMarker());
        }

        assertTrue(missing.isEmpty(),
                "The sample is the only committed example of the marker formats, and these have none: " + missing);
    }

    @Test
    public void everyMarkerInTheSampleParses() {
        read(demo().resolve(DirectoryType.TP.getMarker()), TestProjectMarker.class);

        for (final Path marker : filesNamed(demo(), DirectoryType.TS.getMarker())) {
            read(marker, TestSetMarker.class);
        }

        for (final Path marker : filesNamed(demo(), DirectoryType.TR.getMarker())) {
            read(marker, TestRunMarker.class);
        }
    }

    @Test
    public void everyTestCaseParsesAndCarriesARank() {
        final @NotNull List<Path> cases = caseFiles();
        assertFalse(cases.isEmpty(), "The sample has no test cases, so it demonstrates nothing");

        for (final Path file : cases) {
            final @NotNull TestCaseDto tc = read(file, TestCaseDto.class);

            assertEquals(tc.getId().toString(), file.getFileName().toString().replace(".json", ""),
                    "A case's file name is its identity, so the sample must agree with itself: " + file);
            assertFalse(tc.getOrder().isEmpty(),
                    "The sample is what a project written by the current build looks like, and that means ranked: " + file);
            assertFalse(tc.getDescription().isBlank(),
                    "A case with no description generates no method, which is not what this sample is showing: " + file);
        }
    }

    /**
     * The runs parse, and each is where the plugin will look for it.
     * <p>
     * The folders are found by their marker and the results by
     * {@link TestRunDirectoryDto#resultsFile}, so this asks where a run's results
     * live rather than repeating the answer - repeating it is what lost them.
     * The name used to be the folder's own, so renaming a cycle moved the folder
     * and left the results behind, emptying the run at the next index (#177). A
     * sample folder still carrying a {@code <name>.json} is one this rename never
     * reached.
     */
    @Test
    public void everyRunParsesAndItsResultsNameCasesThatExist() {
        final @NotNull List<String> caseIds = caseFiles().stream()
                .map(file -> file.getFileName().toString().replace(".json", ""))
                .toList();

        final @NotNull List<Path> runs = runFolders();
        assertEquals(runs.size(), 2, "The sample is meant to carry two runs, and carries " + runs);

        for (final Path folder : runs) {
            final @NotNull Path file = TestRunDirectoryDto.resultsFile(folder);

            assertEquals(jsonFilesIn(folder), List.of(file),
                    "A run folder holds exactly one .json, and its name does not depend on the folder's. A file named"
                            + " after the folder is the old format, which no read has looked for since #177: " + folder);

            final @NotNull TestRunDto run = read(file, TestRunDto.class);

            assertFalse(run.getResults().isEmpty(), "A run with no results shows nothing: " + file);

            run.getResults().forEach(item -> assertTrue(caseIds.contains(item.getId().toString()),
                    "A result in " + file.getFileName() + " names a case the sample does not hold: " + item.getId()));
        }
    }

    /**
     * The sample's run folders, found by the marker that makes them runs rather
     * than by the names they happen to have.
     */
    private static @NotNull List<Path> runFolders() {
        return filesNamed(demo(), DirectoryType.TR.getMarker()).stream().map(Path::getParent).toList();
    }

    private static @NotNull List<Path> jsonFilesIn(final @NotNull Path folder) {
        try (Stream<Path> children = Files.list(folder)) {
            return children.filter(path -> path.getFileName().toString().endsWith(".json")).toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not list the run folder " + folder + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * The sample's {@code testin.yml} still names the sample's own project.
     * <p>
     * Opening it in a sandbox whose Testin root points somewhere else rebinds it,
     * and the plugin writes the new name straight back into the committed file -
     * which is correct behaviour and a terrible thing to commit by accident. It
     * happened the first time this sample was opened: the file came back naming a
     * real project on the machine that opened it.
     * <p>
     * So the binding is asserted rather than trusted. A developer who rebinds the
     * sample while trying something finds out here instead of in review.
     */
    @Test
    public void theSampleStillNamesItsOwnProject() {
        final @NotNull Path config = samples().resolve("automation").resolve("testin.yml");

        final @NotNull List<String> bindings;
        try {
            bindings = Files.readAllLines(config).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("testinProject:"))
                    .toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + config + ": " + ex.getMessage(), ex);
        }

        assertEquals(bindings, List.of("testinProject: Demo"),
                "The sample must stay bound to its own project. Opening it against another Testin root rebinds it and rewrites this file.");
    }

    private static @NotNull List<Path> caseFiles() {
        try (Stream<Path> walk = Files.walk(demo().resolve("Test Cases"))) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not walk the sample's test cases: " + ex.getMessage(), ex);
        }
    }
}
