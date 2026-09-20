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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
import org.testin.model.FileKind;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitDiffProcessor {
    // UC-SHARE-010
    public static @NotNull List<PendingChange> getPendingChanges(final @NotNull Project p, final @NotNull Path repositoryRoot) {
        final @NotNull Path root = repositoryRoot.toAbsolutePath().normalize();
        final @NotNull GitRepositoryService repositories = new GitRepositoryService(p);

        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        return toDiffs(repositories.status(root), root,
                Services.getInstance(p, Mapper.class),
                path -> repositories.showAtHead(root, path),
                indexer::findTestCase);
    }

    // UC-SHARE-010
    static @NotNull List<PendingChange> toDiffs(final @NotNull List<String> statusLines, final @NotNull Path repositoryRoot, final @NotNull Mapper mapper, final @NotNull Function<String, String> committedContent, final @NotNull Function<UUID, Optional<TestCaseDto>> cases) {
        final @NotNull Path root = repositoryRoot.toAbsolutePath().normalize();
        final @NotNull List<PendingChange> result = new ArrayList<>();

        for (final GitRefs.StatusEntry entry : GitRefs.parseStatus(statusLines)) {
            final @NotNull Path relativePath = Path.of(entry.path());

            if (FileKind.of(relativePath, folderKindOf(root, relativePath)) == FileKind.SCREENSHOT) continue;

            if (entry.type() == DiffType.ADDED && !Files.exists(root.resolve(relativePath))) {
                Logger.warn("Skipping " + relativePath + ": Git listed it as new, and it is gone");
                continue;
            }

            try {
                result.add(PendingChangeFactory.fromFile(
                        entry.type(),
                        entry.type() == DiffType.ADDED ? "" : committedContent.apply(entry.path()),
                        workingContent(root, relativePath, entry),
                        relativePath,
                        mapper,
                        cases));

            } catch (final RuntimeException ex) {
                Logger.warn("Listing " + relativePath + " without detail: " + ex.getMessage());
                result.add(PendingChangeFactory.unreadable(entry.type(), relativePath));
            }
        }
        return result;
    }

    // Rule-INTERNAL-011
    private static @NotNull DirectoryType folderKindOf(final @NotNull Path repositoryRoot, final @NotNull Path relativePath) {
        final @NotNull Optional<Path> folder = Optional.ofNullable(repositoryRoot.resolve(relativePath).getParent());
        final boolean isRun = folder.filter(at -> Files.exists(at.resolve(DirectoryType.TR.getMarker()))).isPresent();

        return isRun ? DirectoryType.TR : DirectoryType.TRD;
    }

    private static @NotNull String workingContent(final @NotNull Path root, final @NotNull Path relativePath, final @NotNull GitRefs.StatusEntry entry) {
        if (entry.type() == DiffType.DELETED) return "";

        final @NotNull Path file = root.resolve(relativePath);
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            Logger.warn("Could not read changed file " + file + ": " + ex.getMessage());
            throw new IllegalStateException("Could not read " + relativePath, ex);
        }
    }
}
