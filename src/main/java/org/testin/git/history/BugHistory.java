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
import org.testin.indexer.TestCaseFile;
import org.testin.indexer.TestRuns;
import org.testin.logger.Logger;
import org.testin.model.FileKind;
import org.testin.model.result.TestRunItems;
import org.testin.services.Services;
import org.testin.util.FailureText;
import org.testin.util.Mapper;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BugHistory {
    private static final @NotNull String FORMAT = "--format=%x1e%H%x1f%an%x1f%aI";

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-100, Rule-VIEW-PANEL-106, Rule-VIEW-PANEL-107
    public static @NotNull List<BugCard> read(final @NotNull Project p, final @NotNull TestCaseFile file, final @NotNull UUID testCaseId) {
        final @NotNull Path testProject = file.testProject();
        final @NotNull Map<String, TestRunItems> now = runItemsNow(p, testProject, testCaseId);
        final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);
        final @NotNull GitRepositoryService git = new GitRepositoryService(p);
        if (git.isNotRepository(testProject)) return notCommitted(testProject, Map.of(), now, mapper);

        try {
            final @NotNull List<BugCommit> commits = commits(git.log(testProject, FORMAT, "--name-status", "--", ":(glob)**/" + FileKind.RUN_ITEM.fileName(testCaseId)));
            final @NotNull Map<String, String> versions = commits.isEmpty()
                    ? Map.of()
                    : git.objects(testProject, commits.stream().flatMap(BugCommit::objectNames).distinct().toList());

            final @NotNull List<BugCard> cards = new ArrayList<>(notCommitted(testProject, atHead(commits, versions), now, mapper));
            commits.forEach(commit -> commit.files().forEach(changed -> card(testProject, commit, changed, versions, mapper).ifPresent(cards::add)));
            return cards;
        } catch (final GitFailed ex) {
            Logger.warn("Could not read the bugs of " + testCaseId + " from Git: " + FailureText.of(ex));
            return List.of();
        }
    }

    private static @NotNull Map<String, TestRunItems> runItemsNow(final @NotNull Project p, final @NotNull Path testProject, final @NotNull UUID testCaseId) {
        final @NotNull Map<String, TestRunItems> now = new HashMap<>();
        Services.getInstance(p, TestRuns.class).getAllTestRuns().forEach((testRun, tr) -> {
            if (testRun.startsWith(testProject)) {
                tr.resultOf(testCaseId).ifPresent(item -> now.put(inGit(testProject.relativize(testRun.resolve(FileKind.RUN_ITEM.fileName(testCaseId)))), item));
            }
        });
        return now;
    }

    static @NotNull List<BugCommit> commits(final @NotNull String log) {
        return Arrays.stream(log.split(TestCaseHistory.RECORD)).filter(record -> !record.isBlank()).map(BugHistory::commit).toList();
    }

    private static @NotNull BugCommit commit(final @NotNull String record) {
        final @NotNull List<String> lines = record.lines().filter(line -> !line.isBlank()).toList();
        final String @NotNull [] fields = lines.getFirst().split(TestCaseHistory.FIELD, -1);

        return new BugCommit(fields[0], fields[1], ZonedDateTime.parse(fields[2]), lines.stream().skip(1).map(ChangedFile::of).toList());
    }

    private static @NotNull Map<String, String> atHead(final @NotNull List<BugCommit> commits, final @NotNull Map<String, String> versions) {
        final @NotNull Map<String, String> head = new HashMap<>();
        final @NotNull Set<String> seen = new HashSet<>();

        for (final BugCommit commit : commits) {
            for (final ChangedFile changed : commit.files()) {
                if (!changed.after().isEmpty() && seen.add(changed.after())) {
                    head.put(changed.after(), versions.getOrDefault(commit.hash() + ":" + changed.after(), ""));
                }
                if (!changed.before().isEmpty()) seen.add(changed.before());
            }
        }
        return head;
    }

    // Rule-VIEW-PANEL-100, Rule-VIEW-PANEL-107
    private static @NotNull List<BugCard> notCommitted(final @NotNull Path testProject, final @NotNull Map<String, String> head, final @NotNull Map<String, TestRunItems> now, final @NotNull Mapper mapper) {
        return Stream.concat(head.keySet().stream(), now.keySet().stream())
                .collect(Collectors.toCollection(TreeSet::new)).stream()
                .flatMap(path -> BugEvents.between(testRun(testProject, path), parsed(mapper, head.getOrDefault(path, "")), Optional.ofNullable(now.get(path))).stream())
                .map(BugCard::notCommitted)
                .toList();
    }

    // Rule-VIEW-PANEL-105, Rule-VIEW-PANEL-106
    private static @NotNull Optional<BugCard> card(final @NotNull Path testProject, final @NotNull BugCommit commit, final @NotNull ChangedFile changed, final @NotNull Map<String, String> versions, final @NotNull Mapper mapper) {
        final @NotNull Optional<TestRunItems> before = version(mapper, versions, commit.hash() + "^", changed.before());
        final @NotNull Optional<TestRunItems> after = version(mapper, versions, commit.hash(), changed.after());
        if (before.isEmpty() != changed.before().isEmpty() || after.isEmpty() != changed.after().isEmpty()) return Optional.empty();

        return BugEvents.between(testRun(testProject, changed.after().isEmpty() ? changed.before() : changed.after()), before, after)
                .map(event -> new BugCard(commit.hash(), commit.who(), commit.when(), event));
    }

    private static @NotNull Optional<TestRunItems> version(final @NotNull Mapper mapper, final @NotNull Map<String, String> versions, final @NotNull String revision, final @NotNull String path) {
        return path.isEmpty() ? Optional.empty() : parsed(mapper, versions.getOrDefault(revision + ":" + path, ""));
    }

    private static @NotNull Path testRun(final @NotNull Path testProject, final @NotNull String runItem) {
        final @NotNull Path file = testProject.resolve(runItem);
        return Optional.ofNullable(file.getParent()).orElse(testProject);
    }

    private static @NotNull Optional<TestRunItems> parsed(final @NotNull Mapper mapper, final @NotNull String json) {
        if (json.isBlank()) return Optional.empty();

        try {
            return Optional.of(mapper.readValue(json, TestRunItems.class));
        } catch (final UncheckedIOException unreadable) {
            return Optional.empty();
        }
    }

    private static @NotNull String inGit(final @NotNull Path relative) {
        return relative.toString().replace('\\', '/');
    }
}
