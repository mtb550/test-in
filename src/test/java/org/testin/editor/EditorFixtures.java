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

package org.testin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.editor.testcase.TestCaseEditor;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EditorFixtures {

    public static @NotNull TestProjectDirectoryDto testProject(final @NotNull Project p, final @NotNull Path root) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestProjectDirectoryDto tp = Services.getInstance(p, DirectoryMapper.class).setTestProjectNode(root.resolve("NAFATH"));
            Services.getInstance(p, Nodes.class).addTestProject(tp);
            return tp;
        });
    }

    public static @NotNull TestSetDirectoryDto testSet(final @NotNull Project p, final @NotNull TestProjectDirectoryDto tp, final @NotNull String name) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestSetDirectoryDto ts = Services.getInstance(p, DirectoryMapper.class).getTestSetNode(tp.getTestCasesDirectory().getPath().resolve(name), tp.getTestCasesDirectory());
            Services.getInstance(p, Nodes.class).addTestSet(ts);
            return ts;
        });
    }

    public static @NotNull TestCaseDto testCase(final @NotNull Project p, final @NotNull TestSetDirectoryDto ts, final @NotNull String description, final @NotNull String order) {
        final @NotNull TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description(description).order(order).build();
        tc.setParent(ts);
        Services.getInstance(p, TestCases.class).putTestCaseVerbatim(ts.getPath(), tc);
        return tc;
    }

    public static @NotNull List<TestCaseDto> testCases(final @NotNull Project p, final @NotNull TestSetDirectoryDto ts, final int count) {
        final @NotNull List<TestCaseDto> made = new ArrayList<>();
        for (int i = 0; i < count; i++) made.add(testCase(p, ts, "Test case number " + (i + 1), String.format("m%04d", i)));
        return made;
    }

    public static @NotNull TestRunDirectoryDto testRun(final @NotNull Project p, final @NotNull TestProjectDirectoryDto tp, final @NotNull List<TestRunItems> results) {
        final @NotNull TestRunDirectoryDto tr = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunDirectoryDto made = Services.getInstance(p, DirectoryMapper.class).setTestRunNode(tp.getTestRunsDirectory().getPath().resolve("Cycle-1"), tp.getTestRunsDirectory());
            Services.getInstance(p, Nodes.class).addTestRunDir(made);
            return made;
        });

        Services.getInstance(p, TestRuns.class).putTestRun(tr.getPath(), new TestRunDto().setResults(new ArrayList<>(results)));
        return tr;
    }

    public static @NotNull TestRunItems pending(final @NotNull TestCaseDto tc) {
        return new TestRunItems().setId(tc.getId());
    }

    public static @NotNull TestCaseEditor openTestCaseEditor(final @NotNull Project p, final @NotNull TestSetDirectoryDto ts, final @NotNull Disposable owner) {
        final @NotNull TestCaseEditor editor = new TestCaseEditor(p, new UnifiedVirtualFile(ts));
        Disposer.register(owner, editor);

        Await.until("the test case editor never loaded", () -> !editor.isLoading());
        return editor;
    }

    public static @NotNull TestRunEditor openTestRunEditor(final @NotNull Project p, final @NotNull TestRunDirectoryDto tr, final @NotNull Disposable owner) {
        final @NotNull TestRunEditor editor = new TestRunEditor(p, new UnifiedVirtualFile(tr));
        Disposer.register(owner, editor);

        Await.until("the test run editor never loaded", () -> editor.run().isPresent());
        return editor;
    }
}
