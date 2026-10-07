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
import org.testin.model.TestRunDto;
import org.testin.model.result.TestRunItems;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.FailureText;
import org.testin.util.Mapper;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestRunFromGit {
    private static final @NotNull String FORMAT = "--format=%x1e%H";
    private static final @NotNull String DELETED = "D";

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    public static void read(final @NotNull Project p, final @NotNull Path testRunPath) {
        final @NotNull GitRepositoryService git = new GitRepositoryService(p);
        WatchedPath.testProjectOf(testRunPath, Services.getInstance(p, TestinRoot.class).absolutePath())
                .filter(testProject -> !git.isNotRepository(testProject))
                .ifPresent(testProject -> {
                    final @NotNull TestRuns testRuns = Services.getInstance(p, TestRuns.class);
                    final @NotNull TestRunDto testRun = testRuns.getTestRunByPath(testRunPath);
                    readTheCommit(p, git, testProject, testRuns.commitOf(testRunPath), testRun.coveredIds());
                    readTheDeleted(p, git, testProject, testRun.getResults().stream().filter(TestRunItems::isRemoved).map(TestRunItems::getId).toList());
                });
    }

    // Rule-EDITOR-PANEL-126, Rule-EDITOR-PANEL-239
    public static void readAll(final @NotNull Project p) {
        Services.getInstance(p, TestRuns.class).getAllTestRuns().keySet().forEach(testRunPath -> read(p, testRunPath));
    }

    // Rule-EDITOR-PANEL-239
    private static void readTheCommit(final @NotNull Project p, final @NotNull GitRepositoryService git, final @NotNull Path testProject, final @NotNull String commit, final @NotNull Set<UUID> testCaseIds) {
        final @NotNull TestRuns testRuns = Services.getInstance(p, TestRuns.class);
        if (commit.isEmpty() || testRuns.hasRecorded(commit)) return;

        final @NotNull Map<String, UUID> wanted = testCaseIds.stream().collect(Collectors.toMap(FileKind.TEST_CASE::fileName, Function.identity()));
        try {
            final @NotNull Map<String, String> files = git.contents(testProject, commit, git.files(testProject, commit, DirectoryType.TCD.getFolderName()).stream()
                    .filter(path -> wanted.containsKey(fileName(path)))
                    .toList());

            final @NotNull Map<UUID, TestCaseDto> testCases = new HashMap<>();
            files.forEach((path, json) -> parsed(p, json).ifPresent(tc -> testCases.put(wanted.get(fileName(path)), tc)));
            testRuns.rememberRecorded(commit, testCases);
        } catch (final GitFailed ex) {
            Logger.info("The test cases of commit " + commit + " could not be read, so the test cases are shown as they are now: " + FailureText.of(ex));
        }
    }

    // Rule-EDITOR-PANEL-126
    private static void readTheDeleted(final @NotNull Project p, final @NotNull GitRepositoryService git, final @NotNull Path testProject, final @NotNull List<UUID> deleted) {
        if (deleted.isEmpty()) return;

        final @NotNull Map<String, UUID> wanted = deleted.stream().collect(Collectors.toMap(FileKind.TEST_CASE::fileName, Function.identity()));
        try {
            final @NotNull Map<String, UUID> lastVersions = lastVersions(git.log(testProject, Stream.concat(Stream.of(FORMAT, "--name-status", "--"),
                    deleted.stream().map(id -> BugHistory.pathspec(DirectoryType.TCD, FileKind.TEST_CASE, id))).toArray(String[]::new)), wanted);
            if (lastVersions.isEmpty()) return;

            final @NotNull Map<UUID, TestCaseDto> testCases = new HashMap<>();
            git.objects(testProject, List.copyOf(lastVersions.keySet())).forEach((objectName, json) ->
                    parsed(p, json).ifPresent(tc -> testCases.put(lastVersions.get(objectName), tc)));
            Services.getInstance(p, TestRuns.class).rememberLastInGit(testCases);
        } catch (final GitFailed ex) {
            Logger.info("The deleted test cases could not be read from Git, so they show as deleted: " + FailureText.of(ex));
        }
    }

    static @NotNull Map<String, UUID> lastVersions(final @NotNull String log, final @NotNull Map<String, UUID> wanted) {
        final @NotNull Map<String, UUID> found = new HashMap<>();
        for (final String record : log.split(TestCaseHistory.RECORD, -1)) {
            final @NotNull List<String> lines = record.lines().filter(line -> !line.isBlank()).toList();
            if (lines.isEmpty()) continue;

            final @NotNull String hash = lines.getFirst().strip();
            lines.stream().skip(1).map(line -> line.split("\t")).forEach(change -> {
                final @NotNull String path = change[change.length - 1];
                Optional.ofNullable(wanted.get(fileName(path)))
                        .filter(id -> !found.containsValue(id))
                        .ifPresent(id -> found.put((change[0].startsWith(DELETED) ? hash + "^" : hash) + ":" + path, id));
            });
        }
        return found;
    }

    private static @NotNull Optional<TestCaseDto> parsed(final @NotNull Project p, final @NotNull String json) {
        return TestCaseHistory.parsed(Services.getInstance(p, Mapper.class), json);
    }

    private static @NotNull String fileName(final @NotNull String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
