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
}
