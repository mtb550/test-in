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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.NotNull;
import org.testin.TempTree;
import org.testin.model.FileKind;
import org.testin.model.TestRunItems;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Renaming a test run keeps its results (#177).
 * <p>
 * The results used to be named after the folder holding them -
 * {@code Cycle-1/Cycle-1.json} - which made a run the only node in the tree whose
 * contents were named after it, and so the only node a rename could empty. It
 * did: the code that wrote the results derived that name, the scan that read
 * them derived it again, and neither told the rename. Renaming a cycle moved
 * the folder, left the results behind under the old name, and the next index
 * found nothing where a whole cycle had been.
 * <p>
 * What actually fixes that is not the rename learning to carry the file, but the
 * name having nothing to keep in step with - so what is pinned here is the
 * property rather than the operation: <b>a result is named by the test case it is
 * about, so nothing in its name depends on the folder.</b> That one sentence is the bug and the
 * fix. It fails against the old implementation and passes against this one, which
 * is what makes it the regression test.
 * <p>
 * Deliberately not a test of {@code ProjectIndexer.renameNode} itself. That needs
 * the VFS inside a write action, and this repository has no platform test
 * harness (#108). The rename is now three lines that know nothing about test
 * runs. The part that was ever specific to a run is the naming, and the naming
 * is here.
 * The folder rename below is therefore a real one on disk, standing in for the
 * VFS operation that performs it in the IDE.
 * <p>
 * What a run written by an older build does is not here: {@code
 * FormatConversionIdeTest} asserts it, because since #305 it is the converter's
 * answer rather than the reader's - the old-format run is removed on the first
 * open (D4), not left on disk for nothing to read.
 */
public class RunResultsSurviveRenameTest {

    /**
     * Enough of a run to be worth losing: one recorded verdict, which is what a
     * tester would not get back.
     */
    private static final @NotNull UUID JUDGED_CASE = UUID.fromString("11111111-1111-4111-8111-111111111101");

    private static final @NotNull String A_RESULT = """
            {
              "id" : "11111111-1111-4111-8111-111111111101",
              "status" : "PASSED",
              "actualResult" : "Signed in and reached the dashboard."
            }""";

    private static @NotNull Path createRun(final @NotNull Path folder) {
        write(folder.resolve(".tr"), "{}");
        write(folder.resolve(FileKind.RUN_ITEM.fileName(JUDGED_CASE)), A_RESULT);
        return folder;
    }

    private static @NotNull Path tempRoot() {
        try {
            return Files.createTempDirectory("testin-rename");
        } catch (final IOException ex) {
            throw new AssertionError("Could not create a temporary directory to rename in: " + ex.getMessage(), ex);
        }
    }

    private static void write(final @NotNull Path file, final @NotNull String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        } catch (final IOException ex) {
            throw new AssertionError("Could not write " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull Path move(final @NotNull Path from, final @NotNull Path to) {
        try {
            return Files.move(from, to);
        } catch (final IOException ex) {
            throw new AssertionError("Could not rename " + from + " to " + to + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Read the way the indexer reads it, so a format change fails here too rather
     * than only in the sandbox.
     */
    private static @NotNull TestRunItems read(final @NotNull Path file) {
        try {
            return new ObjectMapper().registerModule(new JavaTimeModule()).readValue(file.toFile(), TestRunItems.class);
        } catch (final IOException ex) {
            throw new AssertionError("The run at " + file + " no longer parses: " + ex.getMessage(), ex);
        }
    }

    @Test
    public void aResultIsNamedByItsTestCaseAndNotByItsFolder() {
        assertEquals(FileKind.RUN_ITEM.fileName(JUDGED_CASE), JUDGED_CASE + ".ri",
                "A result is named by the case it is about, so there is no folder name in it to keep in step."
                        + " The moment a result's name is derived from its folder, renaming a run empties it.");
    }

    /**
     * The whole defect, end to end, on a real directory: write a run, rename its
     * folder the way a tester does, and read the results back from where the scan
     * would look for them.
     */
    @Test
    public void renamingTheFolderKeepsTheResultsWhereTheScanLooks() {
        final @NotNull Path root = tempRoot();

        try {
            final @NotNull Path cycle = createRun(root.resolve("Cycle-1"));
            final @NotNull Path renamed = move(cycle, root.resolve("Cycle-1 (rerun)"));

            final @NotNull Path result = renamed.resolve(FileKind.RUN_ITEM.fileName(JUDGED_CASE));
            assertTrue(Files.exists(result),
                    "After a rename the scan looks for " + result.getFileName() + " and it has to be there,"
                            + " or every verdict in the run is gone at the next index");

            assertEquals(read(result).getStatus().name(), "PASSED",
                    "The verdict the tester recorded before the rename is the thing this issue was about");

            assertTrue(Files.exists(renamed.resolve(".tr")), "The marker moved with the folder, as it always did");
        } finally {
            TempTree.delete(root);
        }
    }
}
