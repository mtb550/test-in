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

package org.testin.testcase;

import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class TestCaseSnapshotIdeTest extends BasePlatformTestCase {

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

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-snapshot");
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

    private TestSetDirectoryDto checkoutSet() {
        return WriteAction.computeAndWait(() -> {
            final DirectoryMapper mapper = Services.getInstance(getProject(), DirectoryMapper.class);

            final TestProjectDirectoryDto tp = mapper.setTestProjectNode(getProject(), root.resolve("NAFATH"));
            indexer().addTestProject(tp);

            final TestSetDirectoryDto ts = mapper.getTestSetNode(getProject(), tp.getTestCasesDirectory().getPath().resolve("Checkout"), tp.getTestCasesDirectory());
            indexer().addTestSet(ts);
            return ts;
        });
    }

    public void testUndoingAPasteWhoseSourceSetIsGoneLeavesTheTestCasesInTheDestination() {
        final TestSetDirectoryDto destination = checkoutSet();

        final Path source = destination.getPath().resolveSibling("Login");

        final TestCaseDto moved = TestCaseDto.builder()
                .id(UUID.randomUUID())
                .description("Log in with a valid user")
                .order("m")
                .build();
        moved.setParent(destination);
        final List<UUID> ids = List.of(moved.getId());

        final TestCaseSnapshot sourceBefore = new TestCaseSnapshot(getProject(), source, List.of(moved), List.of());
        final TestCaseSnapshot destinationBefore = TestCaseSnapshot.of(getProject(), destination.getPath(), ids);

        indexer().putTestCaseVerbatim(destination.getPath(), moved);

        final UndoScope scope = UndoScope.of(destination.getPath());
        TestCaseSnapshot.record(getProject(), scope, "Paste",
                List.of(sourceBefore, destinationBefore),
                List.of(TestCaseSnapshot.of(getProject(), source, ids), TestCaseSnapshot.of(getProject(), destination.getPath(), ids)));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertFalse("an undo into a set no longer indexed was not refused",
                Services.getInstance(getProject(), UndoHistories.class).undo(scope));
        assertTrue("the pasted case was taken out of the destination by a refused undo",
                indexer().findTestCase(moved.getId()).isPresent());
    }

    public void testUndoingARemovalPutsTheTestCaseBack() {
        final TestSetDirectoryDto ts = checkoutSet();
        final TestCaseDto removed = TestCaseDto.builder()
                .id(UUID.randomUUID())
                .description("Pay with a saved card")
                .order("m")
                .build();
        removed.setParent(ts);
        indexer().putTestCaseVerbatim(ts.getPath(), removed);
        final List<UUID> ids = List.of(removed.getId());

        final TestCaseSnapshot before = TestCaseSnapshot.of(getProject(), ts.getPath(), ids);
        assertTrue("the removal being undone did not happen", indexer().removeTestCase(ts.getPath(), removed.getId()));

        final UndoScope scope = UndoScope.of(ts.getPath());
        TestCaseSnapshot.record(getProject(), scope, "Remove", List.of(before), List.of(TestCaseSnapshot.of(getProject(), ts.getPath(), ids)));
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertTrue("the undo said it could not put the case back", Services.getInstance(getProject(), UndoHistories.class).undo(scope));
        assertTrue("the case is not back in the index", indexer().findTestCase(removed.getId()).isPresent());
        assertTrue("the case's file is not back on disk", Files.isRegularFile(ts.getPath().resolve(removed.getId() + ".tc")));
    }
}
