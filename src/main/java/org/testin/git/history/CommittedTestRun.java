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

package org.testin.git.history;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.git.GitFailed;
import org.testin.git.GitRepositoryService;
import org.testin.indexer.TestRuns;
import org.testin.indexer.WatchedPath;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
import org.testin.model.FileKind;
import org.testin.model.TestCaseDto;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.FailureText;
import org.testin.util.Mapper;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommittedTestRun {
    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-239
    public static void read(final @NotNull Project p, final @NotNull Path testRunPath) {
        final @NotNull TestRuns testRuns = Services.getInstance(p, TestRuns.class);
        final @NotNull String commit = testRuns.commitOf(testRunPath);
        if (commit.isEmpty() || testRuns.hasRecorded(commit)) return;

        WatchedPath.testProjectOf(testRunPath, Services.getInstance(p, TestinRoot.class).absolutePath())
                .ifPresent(testProject -> read(p, testProject, commit, testRuns.getTestRunByPath(testRunPath).coveredIds()));
    }

    // Rule-EDITOR-PANEL-239
    private static void read(final @NotNull Project p, final @NotNull Path testProject, final @NotNull String commit, final @NotNull Set<UUID> testCaseIds) {
        final @NotNull GitRepositoryService git = new GitRepositoryService(p);
        if (git.isNotRepository(testProject)) return;

        final @NotNull Map<String, UUID> wanted = testCaseIds.stream().collect(Collectors.toMap(FileKind.TEST_CASE::fileName, Function.identity()));
        try {
            final @NotNull Map<String, String> files = git.contents(testProject, commit, git.files(testProject, commit, DirectoryType.TCD.getFolderName()).stream()
                    .filter(path -> wanted.containsKey(fileName(path)))
                    .toList());

            final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);
            final @NotNull Map<UUID, TestCaseDto> testCases = new HashMap<>();
            files.forEach((path, json) -> TestCaseHistory.parsed(mapper, json).ifPresent(tc -> testCases.put(wanted.get(fileName(path)), tc)));
            Services.getInstance(p, TestRuns.class).rememberRecorded(commit, testCases);
        } catch (final GitFailed ex) {
            Logger.info("The test cases of commit " + commit + " could not be read, so the test cases are shown as they are now: " + FailureText.of(ex));
        }
    }

    private static @NotNull String fileName(final @NotNull String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
