/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.indexer;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.model.DirectoryType;
import org.testin.model.FileKind;
import org.testin.model.markers.TestProjectMarker;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * UC-INTERNAL-008, Rule-INTERNAL-091. Converting a test project written by an
 * older Testin, on a real folder (#305, D4).
 * <p>
 * An IDE test rather than a unit test: the converter reads and writes through the
 * indexer's own files, which take a project - and the point of the conversion is
 * what it leaves on disk.
 */
public class FormatConversionIdeTest extends BasePlatformTestCase {

    private static final UUID NAMED_CASE = UUID.fromString("11111111-1111-4111-8111-111111111101");

    private static final String A_CASE = """
            {
              "id" : "11111111-1111-4111-8111-111111111101",
              "description" : "Sign in with a valid user"
            }""";

    private static final UUID HAND_NAMED_CASE = UUID.fromString("22222222-2222-4222-8222-222222222201");

    private static final String A_HAND_NAMED_CASE = """
            {
              "id" : "22222222-2222-4222-8222-222222222201",
              "description" : "Sign in by hand"
            }""";

    /**
     * A case file nobody can read: a tester's own edit that broke the JSON, which
     * the scan reports rather than dropping.
     */
    private static final String A_BROKEN_CASE = "{ \"description\" : ";

    private static final String AN_OLD_RUN = """
            {
              "results" : [ { "id" : "11111111-1111-4111-8111-111111111101", "status" : "PASSED" } ]
            }""";

    private Path root;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-format");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            deleteTree(root);
        } finally {
            super.tearDown();
        }
    }

    /**
     * The whole conversion: cases renamed, the old run removed, every folder
     * given an id, and the format number written last.
     */
    public void testAnOldProjectBecomesThisFormat() {
        final Path project = anOldProject();

        convert(project);

        final Path set = project.resolve("Test Cases").resolve("Login");
        assertTrue("the case is filed under its id, as a .tc",
                Files.isRegularFile(set.resolve(FileKind.TEST_CASE.fileName(NAMED_CASE))));
        assertFalse("and its old file is gone rather than left beside it",
                Files.exists(set.resolve(NAMED_CASE + ".json")));
        assertTrue("a hand-named case that parses is filed under the id inside it (Rule-INTERNAL-084)",
                Files.isRegularFile(set.resolve(FileKind.TEST_CASE.fileName(HAND_NAMED_CASE))));
        assertTrue("a case file nobody can read keeps its own base name, so the scan goes on reporting it",
                Files.isRegularFile(set.resolve("broken.tc")));

        assertFalse("a run written in the old format is removed, results and all",
                Files.exists(project.resolve("Test Runs").resolve("Cycle-1")));

        assertEquals("the project says which format its files are in", TestProjectMarker.FORMAT, formatOf(project));
        assertFalse("and every folder has an id of its own", idOf(project).isEmpty());
        assertFalse(idOf(set).isEmpty());
    }

    /**
     * Converted once. A second pass reads the marker, sees the number and writes
     * nothing - which is what makes it safe to ask on every scan.
     */
    public void testASecondPassChangesNothing() {
        final Path project = anOldProject();

        convert(project);
        final String after = read(project.resolve(DirectoryType.TP.getMarker()));

        convert(project);
        assertEquals("the second pass rewrote the marker", after, read(project.resolve(DirectoryType.TP.getMarker())));
    }

    /**
     * Rule-INTERNAL-091, S1. The ids are derived from each marker's path and
     * creation time, so two testers converting the same commit write the same
     * bytes and Git has nothing to conflict over.
     */
    public void testTwoMachinesConvertingTheSameProjectAgree() {
        final Path mine = anOldProject();
        final Path theirs = anOldProject("NAFATH-clone");

        convert(mine);
        convert(theirs);

        assertEquals("the same project converted twice has to give the same id",
                idOf(mine.resolve("Test Cases")), idOf(theirs.resolve("Test Cases")));
    }

    /**
     * Rule-INTERNAL-091, S5. A format this build does not know is left alone, and
     * the project says why it cannot be read.
     */
    public void testAProjectFromANewerTestinIsLeftAlone() {
        final Path project = anOldProject();
        write(project.resolve(DirectoryType.TP.getMarker()), "{\n  \"format\" : 99\n}");

        convert(project);

        assertEquals("nothing was written over it", 99, formatOf(project));
        assertTrue("and the project says why it is not read",
                marker(project).whyNotReadable().orElse("").contains("newer"));
    }

    private Path anOldProject() {
        return anOldProject("NAFATH");
    }

    /**
     * A project as 2.12.0-alpha wrote one: cases as {@code .json}, one of them
     * hand-named, a run as one {@code run.json}, and no id in any marker.
     */
    private Path anOldProject(final String name) {
        final Path project = root.resolve(name);

        write(project.resolve(DirectoryType.TP.getMarker()), "{\n  \"status\" : \"ACTIVE\"\n}");
        write(project.resolve("Test Cases").resolve(DirectoryType.TCD.getMarker()), "{}");
        write(project.resolve("Test Cases").resolve("Login").resolve(DirectoryType.TS.getMarker()), "{}");
        write(project.resolve("Test Cases").resolve("Login").resolve(NAMED_CASE + ".json"), A_CASE);
        write(project.resolve("Test Cases").resolve("Login").resolve("login.json"), A_HAND_NAMED_CASE);
        write(project.resolve("Test Cases").resolve("Login").resolve("broken.json"), A_BROKEN_CASE);
        write(project.resolve("Test Runs").resolve(DirectoryType.TRD.getMarker()), "{}");
        write(project.resolve("Test Runs").resolve("Cycle-1").resolve(DirectoryType.TR.getMarker()), "{}");
        write(project.resolve("Test Runs").resolve("Cycle-1").resolve("run.json"), AN_OLD_RUN);

        return project;
    }

    private void convert(final Path project) {
        Services.getInstance(Conversions.class).ensure(getProject(), project);
    }

    private int formatOf(final Path project) {
        return marker(project).getFormat();
    }

    private TestProjectMarker marker(final Path project) {
        try {
            return Services.getInstance(getProject(), Mapper.class)
                    .readValue(project.resolve(DirectoryType.TP.getMarker()).toFile(), TestProjectMarker.class);
        } catch (final Exception ex) {
            throw new AssertionError("The project marker no longer parses: " + ex.getMessage(), ex);
        }
    }

    /**
     * The id in whichever marker that folder carries.
     */
    private String idOf(final Path folder) {
        for (final DirectoryType kind : DirectoryType.values()) {
            final Path markerFile = folder.resolve(kind.getMarker());
            if (!Files.isRegularFile(markerFile)) continue;

            try {
                return Services.getInstance(getProject(), Mapper.class)
                        .readValue(markerFile.toFile(), kind.getMarkerClass()).getId();
            } catch (final Exception ex) {
                throw new AssertionError("The marker at " + markerFile + " no longer parses: " + ex.getMessage(), ex);
            }
        }

        throw new AssertionError("No marker in " + folder);
    }

    private static String read(final Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static void write(final Path file, final String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        } catch (final IOException ex) {
            throw new AssertionError("Could not write " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static void deleteTree(final Path folder) {
        try (Stream<Path> walk = Files.walk(folder)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (final IOException ex) {
            System.err.println("Could not clean up " + folder + ": " + ex.getMessage());
        }
    }
}
