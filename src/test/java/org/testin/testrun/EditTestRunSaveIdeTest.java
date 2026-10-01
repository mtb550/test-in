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

package org.testin.testrun;

import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.testin.TempTree;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestRuns;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EditTestRunSaveIdeTest extends BasePlatformTestCase {
    private static final @NotNull UUID JUDGED_WHILE_OPEN = UUID.fromString("22222222-2222-4222-8222-222222222201");
    private static final @NotNull UUID STILL_PENDING = UUID.fromString("22222222-2222-4222-8222-222222222202");
    private static final @NotNull UUID ADDED_BY_THE_EDIT = UUID.fromString("22222222-2222-4222-8222-222222222203");

    private Path root;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-edit-run");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            TempTree.delete(root);
        } finally {
            super.tearDown();
        }
    }

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull TestRunDirectoryDto anOpenRun() {
        final @NotNull TestRunDirectoryDto run = WriteAction.computeAndWait(() -> {
            final @NotNull DirectoryMapper mapper = Services.getInstance(getProject(), DirectoryMapper.class);
            final @NotNull Nodes nodes = Services.getInstance(getProject(), Nodes.class);
            final @NotNull TestProjectDirectoryDto tp = mapper.setTestProjectNode(root.resolve("NAFATH"));
            nodes.addTestProject(tp);

            final @NotNull TestRunDirectoryDto tr = mapper.setTestRunNode(tp.getTestRunsDirectory().getPath().resolve("Cycle-1"), tp.getTestRunsDirectory());
            nodes.addTestRunDir(tr);
            return tr;
        });

        indexedTestRuns().putTestRun(run.getPath(), new TestRunDto().setResults(List.of(
                new TestRunItems().setId(JUDGED_WHILE_OPEN).setStatus(TestStatus.PENDING),
                new TestRunItems().setId(STILL_PENDING).setStatus(TestStatus.PENDING))));
        return run;
    }

    private @NotNull TestStatus statusOf(final @NotNull TestRunDirectoryDto run, final @NotNull UUID testCaseId) {
        return indexedTestRuns().getTestRunByPath(run.getPath()).resultOf(testCaseId).map(TestRunItems::getStatus).orElse(TestStatus.REMOVED);
    }

    private boolean save(final @NotNull TestRunDirectoryDto run) {
        final @NotNull Set<UUID> chosen = Set.of(JUDGED_WHILE_OPEN, STILL_PENDING, ADDED_BY_THE_EDIT);
        final boolean saved = new EditTestRunWork(getProject()).saveEdit(run, run.getName(), chosen, chosen, Map.of());
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        return saved;
    }

    // Rule-TREE-PANEL-128, Rule-TREE-PANEL-074
    public void testSavingKeepsAVerdictThatArrivedWhileTheDialogWasOpen() {
        final @NotNull TestRunDirectoryDto run = anOpenRun();

        indexedTestRuns().changeRun(run.getPath(), held -> held.resultOf(JUDGED_WHILE_OPEN).ifPresent(result -> result.setStatus(TestStatus.FAILED)));

        assertTrue("the edit was refused", save(run));
        assertEquals("the verdict that arrived while the dialog was open was replaced", TestStatus.FAILED, statusOf(run, JUDGED_WHILE_OPEN));
        assertEquals(TestStatus.PENDING, statusOf(run, STILL_PENDING));
        assertEquals("a test case the edit added is not Pending", TestStatus.PENDING, statusOf(run, ADDED_BY_THE_EDIT));
    }

    // Rule-TREE-PANEL-060
    public void testASavedEditCannotBeUndone() {
        final @NotNull TestRunDirectoryDto run = anOpenRun();

        assertTrue("the edit was refused", save(run));
        assertFalse("a saved edit of a test run went onto the tree's undo history",
                Services.getInstance(getProject(), UndoHistories.class).canUndo(UndoScope.TREE));
    }
}
