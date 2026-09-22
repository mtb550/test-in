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
import org.testin.setting.AppSettingsState;
import org.testin.testcase.TestCaseOrder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class TestCaseWritesIdeTest extends BasePlatformTestCase {

    private static final String HAND_NAMED = "Log in by hand.tc";
    private Path root;

    private static void deleteTree(final Path path) {
        if (path == null) return;

        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(each -> {
                try {
                    Files.deleteIfExists(each);
                } catch (final Exception ignored) {
                }
            });
        } catch (final Exception ignored) {
        }
    }

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

    static void undeletable(final Path file) {
        try {
            Files.deleteIfExists(file);
            Files.createDirectories(file);
            Files.writeString(file.resolve("keep"), "in the way");
        } catch (final IOException ex) {
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
        return ts.getPath().resolve(tc.getId() + ".tc");
    }

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

    private ProjectIndexer indexer() {
        return Services.getInstance(getProject(), ProjectIndexer.class);
    }

    private TestSetDirectoryDto oneTestSet() {
        return WriteAction.computeAndWait(() -> {
            final DirectoryMapper mapper = Services.getInstance(getProject(), DirectoryMapper.class);

            final TestProjectDirectoryDto tp = mapper.setTestProjectNode(getProject(), root.resolve("NAFATH"));
            indexer().addTestProject(tp);

            final TestSetDirectoryDto ts = mapper.getTestSetNode(getProject(), tp.getTestCasesDirectory().getPath().resolve("Login"), tp.getTestCasesDirectory());
            indexer().addTestSet(ts);
            return ts;
        });
    }

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

    private Path setWithAHandNamedTestCase() {
        final Path project = SyntheticTree.write(root, 1, 1);
        final Path set = project.resolve("Test Cases").resolve("set-0");

        try (var files = Files.list(set)) {
            final Path testCaseFile = files.filter(file -> file.getFileName().toString().endsWith(".tc")).findFirst().orElseThrow();
            Files.move(testCaseFile, set.resolve(HAND_NAMED));
        } catch (final IOException ex) {
            throw new AssertionError("could not name the case file by hand", ex);
        }

        indexer().scanSingleProject(project);
        return set;
    }

    public void testATestCaseTheSetHasNeverHeldIsWrittenEvenWhenItsRankStays() {
        final TestSetDirectoryDto ts = oneTestSet();
        final TestCaseDto pasted = testCase(ts, "m");

        indexer().updateSequence(ts.getPath(), List.of(pasted), List.of());

        assertTrue("a pasted case whose rank did not move never reached disk",
                Files.isRegularFile(fileOf(ts, pasted)));
    }

    public void testATestCaseSavedAsItIsBeforeTheOrderKeepsItsCreator() {
        final TestSetDirectoryDto ts = oneTestSet();
        final TestCaseDto moved = testCase(ts, "m").setCreatedBy("Sara Al-Otaibi");
        final var createdAt = moved.getCreatedAt();

        indexer().putTestCaseVerbatim(ts.getPath(), moved);
        indexer().updateSequence(ts.getPath(), List.of(moved), List.of());

        final TestCaseDto indexed = indexer().findTestCase(moved.getId()).orElseThrow();
        assertEquals("a moved case took the paster's name as its creator", "Sara Al-Otaibi", indexed.getCreatedBy());
        assertEquals("a moved case took a new creation date", createdAt, indexed.getCreatedAt());
    }

    public void testACreatedTestCaseIsWrittenByTheOrderWriteWithItsRankAndItsCreator() {
        final TestSetDirectoryDto ts = oneTestSet();
        final TestCaseDto created = testCase(ts, "");
        final List<TestCaseDto> arranged = List.of(created);

        indexer().updateSequence(ts.getPath(), arranged, TestCaseOrder.place(arranged));

        final TestCaseDto indexed = indexer().findTestCase(created.getId()).orElseThrow();
        assertFalse("the order write gave the new case no rank", indexed.getOrder().isEmpty());
        assertEquals("the order write did not stamp the new case as created",
                Services.getInstance(getProject(), AppSettingsState.class).testerName, indexed.getCreatedBy());

        try {
            assertTrue("the case's file does not carry the rank it was given",
                    Files.readString(fileOf(ts, created)).contains(indexed.getOrder()));
        } catch (final IOException ex) {
            throw new AssertionError("the new case was not written", ex);
        }
    }

    public void testAMovedTestCaseIsInItsNewSetAndGoneFromTheOld() {
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

    public void testAMoveWhoseWriteIsRefusedLeavesTheTestCaseWhereItWas() {
        final List<TestSetDirectoryDto> sets = twoTestSets();
        final TestSetDirectoryDto login = sets.get(0);
        final TestSetDirectoryDto signUp = sets.get(1);
        final TestCaseDto cut = testCase(login, "m");
        indexer().putTestCaseVerbatim(login.getPath(), cut);

        try {
            Files.createDirectories(fileOf(signUp, cut));
        } catch (final IOException ex) {
            throw new AssertionError("could not set up the refused write", ex);
        }

        assertFalse("a refused write was reported as a move", indexer().moveTestCase(login.getPath(), signUp.getPath(), pastedInto(signUp, cut)));

        assertTrue("a refused move took the case's file out of its set", Files.isRegularFile(fileOf(login, cut)));
        assertEquals("a refused move took the case out of its set in the index",
                login.getPath(), indexer().findTestCase(cut.getId()).orElseThrow().getParent().getPath());
    }

    public void testARemovalWhoseDeleteIsRefusedKeepsTheTestCase() {
        final TestSetDirectoryDto ts = oneTestSet();
        final TestCaseDto tc = testCase(ts, "m");
        indexer().putTestCaseVerbatim(ts.getPath(), tc);

        undeletable(fileOf(ts, tc));

        assertFalse("a refused delete was reported as a removal", indexer().removeTestCase(ts.getPath(), tc.getId()));

        assertTrue("a refused delete took the case out of the index", indexer().findTestCase(tc.getId()).isPresent());
        assertTrue("a refused delete took the case out of its set",
                indexer().getTestCasesForTestSet(ts.getPath()).stream().anyMatch(each -> each.getId().equals(tc.getId())));
    }

    public void testAMoveWhoseOldFileWillNotGoLeavesTheTestCaseWhereItWas() {
        final List<TestSetDirectoryDto> sets = twoTestSets();
        final TestSetDirectoryDto login = sets.get(0);
        final TestSetDirectoryDto signUp = sets.get(1);
        final TestCaseDto cut = testCase(login, "m");
        indexer().putTestCaseVerbatim(login.getPath(), cut);

        undeletable(fileOf(login, cut));

        assertFalse("a move whose old file stayed was reported as a move", indexer().moveTestCase(login.getPath(), signUp.getPath(), pastedInto(signUp, cut)));

        assertFalse("the new file was left behind beside the old one", Files.exists(fileOf(signUp, cut)));
        assertEquals("the index files the case under the set it could not leave",
                login.getPath(), indexer().findTestCase(cut.getId()).orElseThrow().getParent().getPath());
        assertTrue("the new set lists a case that did not arrive",
                indexer().getTestCasesForTestSet(signUp.getPath()).stream().noneMatch(each -> each.getId().equals(cut.getId())));
    }

    public void testSavingAHandNamedTestCaseFilesItUnderItsId() {
        final Path set = setWithAHandNamedTestCase();
        final TestCaseDto tc = indexer().getTestCasesForTestSet(set).getFirst();

        assertTrue("the save said it failed", indexer().putTestCaseVerbatim(set, tc));

        assertTrue("the case was not filed under its id", Files.isRegularFile(set.resolve(tc.getId() + ".tc")));
        assertFalse("the hand-named file was left beside it", Files.exists(set.resolve(HAND_NAMED)));
    }

    public void testASaveWhoseHandNamedFileWillNotGoIsTakenBack() {
        final Path set = setWithAHandNamedTestCase();
        final TestCaseDto tc = indexer().getTestCasesForTestSet(set).getFirst();
        undeletable(set.resolve(HAND_NAMED));

        assertFalse("a save that left the case in two files was reported as saved", indexer().putTestCaseVerbatim(set, tc));

        assertFalse("the file filed under the id was left beside the hand-named one", Files.exists(set.resolve(tc.getId() + ".tc")));
        assertTrue("the hand-named file went although its delete was refused", Files.exists(set.resolve(HAND_NAMED)));
    }
}
