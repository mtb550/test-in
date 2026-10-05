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

package org.testin.git;

import com.intellij.openapi.project.Project;
import git4idea.commands.GitCommand;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.TestCaseFile;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.FailureText;
import org.testin.util.Mapper;

import java.io.UncheckedIOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestCaseHistory {
    static final @NotNull String RECORD = Character.toString(0x1E);
    static final @NotNull String FIELD = Character.toString(0x1F);
    private static final @NotNull String FORMAT = "--format=%x1e%H%x1f%an%x1f%aI%x1f%s";

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-096, Rule-VIEW-PANEL-099, Rule-VIEW-PANEL-100, Rule-VIEW-PANEL-101
    public static @NotNull History read(final @NotNull Project p, final @NotNull TestCaseFile file, final @NotNull TestCaseDto now) {
        if (new GitRepositoryService(p).isNotRepository(file.testProject())) return History.NOT_UNDER_GIT;

        final @NotNull String path = file.inProject().toString().replace('\\', '/');
        try {
            final @NotNull List<HistoryCommit> commits = commits(GitCommandRunner.execute(p, file.testProject(), GitCommand.LOG, "--follow", FORMAT, "--name-only", "--", path), path);
            final @NotNull Map<String, String> versions = commits.isEmpty()
                    ? Map.of()
                    : GitCommandRunner.readObjects(p, file.testProject(), commits.stream().map(HistoryCommit::objectName).toList());

            return History.read(entries(Services.getInstance(p, Mapper.class), commits, versions, now));
        } catch (final GitFailed ex) {
            return History.failed(FailureText.of(ex));
        }
    }

    static @NotNull List<HistoryCommit> commits(final @NotNull String log, final @NotNull String path) {
        return Arrays.stream(log.split(RECORD)).filter(record -> !record.isBlank()).map(record -> commit(record, path)).toList();
    }

    private static @NotNull HistoryCommit commit(final @NotNull String record, final @NotNull String path) {
        final @NotNull List<String> lines = record.lines().filter(line -> !line.isBlank()).toList();
        final String @NotNull [] fields = lines.getFirst().split(FIELD, -1);

        return new HistoryCommit(fields[0], fields[1], ZonedDateTime.parse(fields[2]), fields.length > 3 ? fields[3] : "", lines.size() > 1 ? lines.getLast().strip() : path);
    }

    // Rule-VIEW-PANEL-096, Rule-VIEW-PANEL-098
    static @NotNull List<HistoryEntry> entries(final @NotNull Mapper mapper, final @NotNull List<HistoryCommit> commits, final @NotNull Map<String, String> versions, final @NotNull TestCaseDto now) {
        final @NotNull List<Optional<TestCaseDto>> read = commits.stream().map(commit -> parsed(mapper, versions.getOrDefault(commit.objectName(), ""))).toList();
        final @NotNull List<HistoryEntry> entries = new ArrayList<>();

        uncommitted(read, now).ifPresent(entries::add);
        for (int at = 0; at < commits.size(); at++) {
            entries.add(entry(commits.get(at), read.get(at), at + 1 < read.size() ? read.get(at + 1) : Optional.empty(), at + 1 == commits.size()));
        }
        return entries;
    }

    private static @NotNull Optional<HistoryEntry> uncommitted(final @NotNull List<Optional<TestCaseDto>> read, final @NotNull TestCaseDto now) {
        if (read.isEmpty()) {
            return Optional.of(new HistoryEntry(HistoryEntryKind.CREATED, "", now.getCreatedBy(), now.getCreatedAt(), "", List.of()));
        }

        return read.getFirst()
                .map(committed -> TestCaseChangeComparator.compare(committed, now))
                .filter(changes -> !changes.isEmpty())
                .map(changes -> new HistoryEntry(HistoryEntryKind.CHANGED, "", lastEditor(now), lastEdited(now), "", changes));
    }

    private static @NotNull HistoryEntry entry(final @NotNull HistoryCommit commit, final @NotNull Optional<TestCaseDto> version, final @NotNull Optional<TestCaseDto> before, final boolean oldest) {
        if (version.isEmpty()) return HistoryEntry.of(HistoryEntryKind.UNREADABLE, commit, List.of());
        if (oldest) return HistoryEntry.of(HistoryEntryKind.CREATED, commit, List.of());

        return before.map(older -> HistoryEntry.of(HistoryEntryKind.CHANGED, commit, TestCaseChangeComparator.compare(older, version.orElseThrow())))
                .orElseGet(() -> HistoryEntry.of(HistoryEntryKind.UNCOMPARED, commit, List.of()));
    }

    private static @NotNull Optional<TestCaseDto> parsed(final @NotNull Mapper mapper, final @NotNull String json) {
        if (json.isBlank()) return Optional.empty();

        try {
            return Optional.of(mapper.readValue(json, TestCaseDto.class));
        } catch (final UncheckedIOException unreadable) {
            return Optional.empty();
        }
    }

    private static @NotNull String lastEditor(final @NotNull TestCaseDto now) {
        return now.getUpdatedBy().isBlank() ? now.getCreatedBy() : now.getUpdatedBy();
    }

    private static @NotNull ZonedDateTime lastEdited(final @NotNull TestCaseDto now) {
        return now.getUpdatedBy().isBlank() ? now.getCreatedAt() : now.getUpdatedAt();
    }
}
