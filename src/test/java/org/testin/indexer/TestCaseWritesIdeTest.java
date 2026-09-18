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

import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * UC-INTERNAL-004.
 * <p>
 * What reaches disk when a test set's order is saved: the case the tester just
 * pasted or created, and nothing it did not need.
 * <p>
 * An IDE test because the store writes through the project's services and
 * claims its own writes with the file watcher, and there is no seam that answers
 * the question without them.
 */
public class TestCaseWritesIdeTest extends BasePlatformTestCase {

    private Path root;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-writes");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            deleteTree(root);
        } finally {
            super.tearDown();
        }
    }

    private static void deleteTree(final Path path) {
        if (path == null) return;

        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(each -> {
                try {
                    Files.deleteIfExists(each);
                } catch (final Exception ignored) {
                    // Left for the operating system.
                }
            });
        } catch (final Exception ignored) {
            // Nothing to walk, or nothing to remove.
        }
    }

    private ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    /**
     * A test project with one test set in it, built the way the create actions
     * build them: the mapper makes the node, the indexer is told.
     */
    private TestSetDirectoryDto testSet() {
        return WriteAction.computeAndWait(() -> {
            final DirectoryMapper mapper = Services.getInstance(getProject(), DirectoryMapper.class);

            final TestProjectDirectoryDto tp = mapper.setTestProjectNode(getProject(), root.resolve("NAFATH"));
            indexer().addTestProject(tp);

            final TestSetDirectoryDto ts = mapper.getTestSetNode(getProject(), tp.getTestCasesDirectory().getPath().resolve("Login"), tp.getTestCasesDirectory());
            indexer().addTestSet(ts);
            return ts;
        });
    }

    /**
     * Two test sets in one test project, for a case to be moved between.
     */
    private List<TestSetDirectoryDto> twoTestSets() {
        return WriteAction.computeAndWait(() -> {
            final DirectoryMapper mapper = Services.getInstance(getProject(), DirectoryMapper.class);

            final TestProjectDirectoryDto tp = mapper.setTestProjectNode(getProject(), root.resolve("NAFATH"));
            indexer().addTestProject(tp);

            final TestSetDirectoryDto login = mapper.getTestSetNode(getProject(), tp.getTestCasesDirectory().getPath().resolve("Login"), tp.getTestCasesDirectory());
            final TestSetDirectoryDto signUp = mapper.getTestSetNode(getProject(), tp.getTestCasesDirectory().getPath().resolve("Sign up"), tp.getTestCasesDirectory());
            indexer().addTestSet(login);
            indexer().addTestSet(signUp);
            return List.of(login, signUp);
        });
    }

    /**
     * The case as a cut pastes it: the same id and audit, in the set it goes to.
     */
    private static TestCaseDto pastedInto(final TestSetDirectoryDto ts, final TestCaseDto cut) {
        final TestCaseDto pasted = TestCaseDto.builder()
                .id(cut.getId())
                .description(cut.getDescription())
                .order(cut.getOrder())
                .build()
                .setCreatedBy(cut.getCreatedBy());
        pasted.setParent(ts);
        return pasted;
    }

    /**
     * Puts a folder where a case's file is, with a file inside it held open, so
     * deleting the case's file is refused the way a locked file refuses it. Both
     * halves are needed on Windows, where the tests run: the recycle bin takes a
     * folder whose files are all closed, and a plain delete refuses any folder
     * with something in it. Close what this answers once the call under test
     * has returned.
     */
    private static java.io.FileInputStream undeletable(final Path file) {
        try {
            Files.deleteIfExists(file);
            Files.createDirectories(file);
            final Path inside = file.resolve("keep");
            Files.writeString(inside, "in the way");
            return new java.io.FileInputStream(inside.toFile());
        } catch (final java.io.IOException ex) {
            throw new AssertionError("could not set up the refused delete", ex);
        }
    }

    private static TestCaseDto testCase(final TestSetDirectoryDto ts, final String rank) {
        final TestCaseDto tc = TestCaseDto.builder()
                .id(UUID.randomUUID())
                .description("Log in with a valid user")
                .order(rank)
                .build();
        tc.setParent(ts);
        return tc;
    }

    private static Path fileOf(final TestSetDirectoryDto ts, final TestCaseDto tc) {
        return ts.getPath().resolve(tc.getId() + ".json");
    }

    /**
     * Rule-INTERNAL-031.
     * <p>
     * A case the set has never held is written even when its rank did not have
     * to change. A pasted case keeps the rank it was copied with, and when that
     * rank already sorts last it is not among the moved - so it used to live in
     * memory only, and a cut had deleted its file a moment before (#66, finding
     * 112).
     */
    public void testACaseTheSetHasNeverHeldIsWrittenEvenWhenItsRankStays() {
        final TestSetDirectoryDto ts = testSet();
        final TestCaseDto pasted = testCase(ts, "m");

        indexer().updateSequence(ts.getPath(), List.of(pasted), List.of());

        assertTrue("a pasted case whose rank did not move never reached disk",
                Files.isRegularFile(fileOf(ts, pasted)));
    }

    /**
     * Rule-INTERNAL-035, Rule-EDITOR-PANEL-082.
     * <p>
     * A case saved as it is before its set's order is saved keeps who created
     * it. That is what a pasted cut now does: the cut had taken the case out of
     * the index, so the sequence write saw it for the first time and recorded
     * the paster as its creator (#66, finding 114).
     */
    public void testACaseSavedAsItIsBeforeTheOrderKeepsItsCreator() {
        final TestSetDirectoryDto ts = testSet();
        final TestCaseDto moved = testCase(ts, "m").setCreatedBy("Sara Al-Otaibi");
        final var createdAt = moved.getCreatedAt();

        indexer().putTestCaseVerbatim(ts.getPath(), moved);
        indexer().updateSequence(ts.getPath(), List.of(moved), List.of());

        final TestCaseDto indexed = indexer().findTestCase(moved.getId()).orElseThrow();
        assertEquals("a moved case took the paster's name as its creator", "Sara Al-Otaibi", indexed.getCreatedBy());
        assertEquals("a moved case took a new creation date", createdAt, indexed.getCreatedAt());
    }

    /**
     * Rule-INTERNAL-031, Rule-INTERNAL-034.
     * <p>
     * A new, unranked case is written by the order write alone: ranked, and
     * stamped as created. Creating a test case used to save it directly as well,
     * so the file was written twice - first without its rank (#66, finding 115).
     */
    public void testACreatedCaseIsWrittenByTheOrderWriteWithItsRankAndItsCreator() {
        final TestSetDirectoryDto ts = testSet();
        final TestCaseDto created = testCase(ts, "");
        final List<TestCaseDto> arranged = List.of(created);

        indexer().updateSequence(ts.getPath(), arranged, org.testin.testcase.TestCaseOrder.place(arranged));

        final TestCaseDto indexed = indexer().findTestCase(created.getId()).orElseThrow();
        assertFalse("the order write gave the new case no rank", indexed.getOrder().isEmpty());
        assertEquals("the order write did not stamp the new case as created",
                Services.getInstance(getProject(), org.testin.setting.AppSettingsState.class).testerName, indexed.getCreatedBy());

        try {
            assertTrue("the case's file does not carry the rank it was given",
                    Files.readString(fileOf(ts, created)).contains(indexed.getOrder()));
        } catch (final java.io.IOException ex) {
            throw new AssertionError("the new case was not written", ex);
        }
    }

    /**
     * Rule-INTERNAL-035.
     * <p>
     * A cut pasted into another set is written there, keeps who created it, and
     * leaves the set it came from - in the index and on disk alike.
     */
    public void testAMovedCaseIsInItsNewSetAndGoneFromTheOld() {
        final List<TestSetDirectoryDto> sets = twoTestSets();
        final TestSetDirectoryDto login = sets.get(0);
        final TestSetDirectoryDto signUp = sets.get(1);
        final TestCaseDto cut = testCase(login, "m").setCreatedBy("Sara Al-Otaibi");
        indexer().putTestCaseVerbatim(login.getPath(), cut);

        assertTrue("the move said it failed", indexer().moveTestCase(login.getPath(), signUp.getPath(), pastedInto(signUp, cut)));

        assertTrue("the moved case was not written into its new set", Files.isRegularFile(fileOf(signUp, cut)));
        assertFalse("the moved case's old file was left behind", Files.exists(fileOf(login, cut)));

        final TestCaseDto indexed = indexer().findTestCase(cut.getId()).orElseThrow();
        assertEquals("the index still files the case under its old set", signUp.getPath(), indexed.getParent().getPath());
        assertEquals("the move took a new creator", "Sara Al-Otaibi", indexed.getCreatedBy());
        assertTrue("the old set still lists the case",
                indexer().getTestCasesForTestSet(login.getPath()).stream().noneMatch(tc -> tc.getId().equals(cut.getId())));
    }

    /**
     * Rule-INTERNAL-035.
     * <p>
     * A move whose write is refused leaves the case where it was. The paste used
     * to remove the cut first, so a refused write left the case in neither set
     * (#66, finding 284).
     */
    public void testAMoveWhoseWriteIsRefusedLeavesTheCaseWhereItWas() {
        final List<TestSetDirectoryDto> sets = twoTestSets();
        final TestSetDirectoryDto login = sets.get(0);
        final TestSetDirectoryDto signUp = sets.get(1);
        final TestCaseDto cut = testCase(login, "m");
        indexer().putTestCaseVerbatim(login.getPath(), cut);

        // A folder where the file has to go refuses the write, the way a locked
        // or read-only file does.
        try {
            Files.createDirectories(fileOf(signUp, cut));
        } catch (final java.io.IOException ex) {
            throw new AssertionError("could not set up the refused write", ex);
        }

        assertFalse("a refused write was reported as a move", indexer().moveTestCase(login.getPath(), signUp.getPath(), pastedInto(signUp, cut)));

        assertTrue("a refused move took the case's file out of its set", Files.isRegularFile(fileOf(login, cut)));
        assertEquals("a refused move took the case out of its set in the index",
                login.getPath(), indexer().findTestCase(cut.getId()).orElseThrow().getParent().getPath());
    }

    /**
     * Rule-EDITOR-PANEL-064.
     * <p>
     * A removal whose file will not go leaves the case in its set. The index let
     * go of the case first, so the tester read Removed over a file still on
     * disk, and the next scan brought it back (#66, finding 292).
     */
    public void testARemovalWhoseDeleteIsRefusedKeepsTheCase() {
        final TestSetDirectoryDto ts = testSet();
        final TestCaseDto tc = testCase(ts, "m");
        indexer().putTestCaseVerbatim(ts.getPath(), tc);

        try (var held = undeletable(fileOf(ts, tc))) {
            assertFalse("a refused delete was reported as a removal", indexer().removeTestCase(ts.getPath(), tc.getId()));
        } catch (final java.io.IOException ex) {
            throw new AssertionError("could not let go of the held file", ex);
        }

        assertTrue("a refused delete took the case out of the index", indexer().findTestCase(tc.getId()).isPresent());
        assertTrue("a refused delete took the case out of its set",
                indexer().getTestCasesForTestSet(ts.getPath()).stream().anyMatch(each -> each.getId().equals(tc.getId())));
    }

    /**
     * Rule-INTERNAL-035.
     * <p>
     * A move whose old file will not go takes back the new one, so the case is
     * in one set rather than on disk in two under one id (#66, finding 292).
     */
    public void testAMoveWhoseOldFileWillNotGoLeavesTheCaseWhereItWas() {
        final List<TestSetDirectoryDto> sets = twoTestSets();
        final TestSetDirectoryDto login = sets.get(0);
        final TestSetDirectoryDto signUp = sets.get(1);
        final TestCaseDto cut = testCase(login, "m");
        indexer().putTestCaseVerbatim(login.getPath(), cut);

        try (var held = undeletable(fileOf(login, cut))) {
            assertFalse("a move whose old file stayed was reported as a move", indexer().moveTestCase(login.getPath(), signUp.getPath(), pastedInto(signUp, cut)));
        } catch (final java.io.IOException ex) {
            throw new AssertionError("could not let go of the held file", ex);
        }

        assertFalse("the new file was left behind beside the old one", Files.exists(fileOf(signUp, cut)));
        assertEquals("the index files the case under the set it could not leave",
                login.getPath(), indexer().findTestCase(cut.getId()).orElseThrow().getParent().getPath());
        assertTrue("the new set lists a case that did not arrive",
                indexer().getTestCasesForTestSet(signUp.getPath()).stream().noneMatch(each -> each.getId().equals(cut.getId())));
    }
}
