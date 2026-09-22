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
import org.testin.TempTree;
import org.testin.model.DirectoryType;
import org.testin.model.FileKind;
import org.testin.model.markers.TestProjectMarker;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    /**
     * What a folder holds, by name and in one order, so two conversions of the
     * same folder can be compared.
     */
    private static List<String> namesIn(final Path folder) {
        try (Stream<Path> children = Files.list(folder)) {
            return children.map(file -> String.valueOf(file.getFileName())).sorted().toList();
        } catch (final IOException ex) {
            throw new AssertionError("Could not list " + folder + ": " + ex.getMessage(), ex);
        }
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

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-format");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            TempTree.delete(root);
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
     * Rule-INTERNAL-082, Rule-INTERNAL-091, S1. Two files claiming one id: the
     * first path alphabetically keeps it and the second takes one derived from
     * the id it claimed and its own path inside the project.
     * <p>
     * Derived rather than drawn at random, and this is the branch that says so:
     * the whole folder converts to the same names on both clones, so two testers
     * who convert the same commit have nothing to conflict over. A random id
     * would pass every other assertion here and produce a conflict on every
     * converted case (#288).
     */
    public void testTwoFilesClaimingOneIdConvertTheSameWayOnBothMachines() {
        final Path mine = withACopiedCase(anOldProject());
        final Path theirs = withACopiedCase(anOldProject("NAFATH-clone"));

        convert(mine);
        convert(theirs);

        final Path mySet = mine.resolve("Test Cases").resolve("Login");
        final Path theirSet = theirs.resolve("Test Cases").resolve("Login");

        assertEquals("the same folder converted on two machines has to end up holding the same file names,"
                + " or every converted case is a conflict", namesIn(mySet), namesIn(theirSet));

        assertTrue("the file that claimed the id first kept it",
                Files.isRegularFile(mySet.resolve(FileKind.TEST_CASE.fileName(NAMED_CASE))));
        assertEquals("the copy went over the case it claimed instead of becoming a case of its own",
                4, namesIn(mySet).stream().filter(name -> name.endsWith(".tc")).count());
    }

    /**
     * Rule-INTERNAL-091, S3. A {@code .tp} that is there and will not parse:
     * nothing in the project is touched.
     * <p>
     * The conversion is all or nothing on purpose. A project whose own marker
     * cannot be read is one where the format number cannot be written either. So
     * moving its files would leave a half-converted project that says it is in
     * the old format. Which file to repair is the notification's to say.
     */
    public void testAProjectMarkerThatWillNotParseLeavesEverythingAlone() {
        final Path project = anOldProject();
        final String damaged = "{ \"status\" : ";
        write(project.resolve(DirectoryType.TP.getMarker()), damaged);

        convert(project);

        assertTrue("a test case was converted although the project's own marker could not be read",
                Files.isRegularFile(project.resolve("Test Cases").resolve("Login").resolve(NAMED_CASE + ".json")));
        assertTrue("the old-format run was removed although the project's own marker could not be read",
                Files.isDirectory(project.resolve("Test Runs").resolve("Cycle-1")));
        assertEquals("the damaged marker was written over, which would replace it with defaults",
                damaged, read(project.resolve(DirectoryType.TP.getMarker())));
    }

    /**
     * D4, S2. A folder under Test Runs that Testin did not write is left where it
     * is, whatever it holds.
     * <p>
     * The converter removes what it recognizes rather than what it finds: a run
     * is a folder Testin marked as one, and a tester's own folder of notes is not
     * test data to be wiped because it happens to hold a file of that name.
     */
    public void testAFolderTestinDidNotWriteIsNotRemoved() {
        final Path project = anOldProject();
        final Path notes = project.resolve("Test Runs").resolve("Notes from the cycle");
        write(notes.resolve("run.json"), AN_OLD_RUN);

        convert(project);

        assertTrue("a folder under Test Runs with no marker was removed, and it was never Testin's to remove",
                Files.isRegularFile(notes.resolve("run.json")));
        assertFalse("and the run Testin did write is still there", Files.exists(project.resolve("Test Runs").resolve("Cycle-1")));
    }

    /**
     * D4, S13. A conversion that could not finish leaves the format number out,
     * so the next open tries again.
     * <p>
     * The number is written last and only when every step succeeded. Stamping it
     * over a half-converted project is the one outcome there is no way back from:
     * 2.14.0-alpha deletes the converter, and a project that says it is already
     * in this format is never offered to it again.
     */
    public void testAConversionThatCouldNotFinishIsTriedAgain() {
        final Path project = anOldProject();
        final Path set = project.resolve("Test Cases").resolve("Login");

        // The name the first case has to move to is a folder with something in
        // it, which refuses the move the way a locked file does.
        final Path inTheWay = set.resolve(FileKind.TEST_CASE.fileName(NAMED_CASE));
        write(inTheWay.resolve("keep"), "in the way");

        convert(project);

        assertEquals("a conversion that failed half way wrote the format number anyway, so the next open would"
                + " skip a project whose files this build cannot read", 0, formatOf(project));

        TempTree.delete(inTheWay);
        convert(project);

        assertEquals("the conversion was not tried again once what refused it was gone",
                TestProjectMarker.FORMAT, formatOf(project));
        assertTrue("and the case it could not move the first time is filed under its id now",
                Files.isRegularFile(set.resolve(FileKind.TEST_CASE.fileName(NAMED_CASE))));
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

    /**
     * A second file claiming the id the first one has, the way a case copied on
     * GitHub arrives (#288). Named so it sorts after the file it is a copy of.
     */
    private Path withACopiedCase(final Path project) {
        write(project.resolve("Test Cases").resolve("Login").resolve("copy of login.json"), A_CASE);
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
}
