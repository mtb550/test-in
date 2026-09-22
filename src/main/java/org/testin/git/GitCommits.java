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
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
import org.testin.model.FileKind;
import org.testin.util.Bundle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class GitCommits {
    private final @NotNull Project p;

    private final @NotNull GitRepositoryService repositories;

    public GitCommits(final @NotNull Project p) {
        this.p = p;
        this.repositories = new GitRepositoryService(p);
    }

    static @NotNull Set<String> markersAlongside(final @NotNull Path repositoryPath, final @NotNull Set<String> testCasePaths) {
        final @NotNull Set<String> markers = new LinkedHashSet<>();

        for (final String directory : GitRefs.ancestorDirectories(testCasePaths)) {
            for (final DirectoryType type : DirectoryType.values()) {
                final @NotNull String marker = type.getMarker();
                if (marker.isBlank()) continue;

                final @NotNull String relative = directory.isEmpty() ? marker : directory + "/" + marker;
                if (Files.exists(repositoryPath.resolve(relative))) markers.add(relative);
            }
        }
        return markers;
    }

    // UC-SHARE-012, Rule-SHARE-112
    static @NotNull Set<String> screenshotsAlongside(final @NotNull List<String> statusLines, final @NotNull Set<String> paths) {
        final @NotNull Set<String> runFolders = paths.stream()
                .filter(path -> FileKind.of(Path.of(path)) == FileKind.RUN_ITEM)
                .map(GitCommits::folderOf)
                .collect(Collectors.toSet());

        return GitRefs.parseStatus(statusLines).stream()
                .map(GitRefs.StatusEntry::path)
                .filter(path -> FileKind.of(Path.of(path), DirectoryType.TR) == FileKind.SCREENSHOT)
                .filter(path -> runFolders.contains(folderOf(path)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static @NotNull String folderOf(final @NotNull String path) {
        return Optional.ofNullable(Path.of(path).getParent()).map(Path::toString).orElse("");
    }

    // UC-SHARE-012, Rule-SHARE-055
    static @NotNull Set<String> stageable(final @NotNull Path repositoryPath, final @NotNull Set<String> paths) {
        return paths.stream()
                .filter(path -> Files.exists(repositoryPath.resolve(path)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // UC-SHARE-012, Rule-SHARE-054
    public void stageAndCommit(final @NotNull Path repositoryPath, final @NotNull String message, final @NotNull Collection<PendingChange> selectedChanges) {
        final @NotNull Set<String> paths = GitRefs.repoRelativePaths(selectedChanges);
        if (paths.isEmpty()) throw new IllegalArgumentException("No Git changes were selected");

        paths.addAll(screenshotsAlongside(repositories.status(repositoryPath), paths));
        paths.addAll(markersAlongside(repositoryPath, paths));

        final @NotNull Set<String> stageable = stageable(repositoryPath, paths);
        if (!stageable.isEmpty()) {
            GitCommandRunner.executeOverPaths(p, repositoryPath, stageable, "git", "add");
        }

        GitCommandRunner.executeOverPaths(p, repositoryPath, paths, "git", "commit", "--only", "-m", message);
    }

    public @NotNull String headCommitId(final @NotNull Path repositoryPath) {
        try {
            return GitCommandRunner.execute(p, repositoryPath, "git", "rev-parse", "--short", "HEAD").trim();
        } catch (final RuntimeException ex) {
            Logger.warn("Could not read the commit id: " + ex.getMessage());
            return "";
        }
    }

    public void pullAndPush(final @NotNull Path repositoryPath, final @NotNull String remote, final @NotNull String branch) {
        pullWhereTheRemoteHasBranch(repositoryPath, remote, branch);
        push(repositoryPath, remote, branch);
    }

    // UC-SHARE-016, Rule-SHARE-069
    public void pullWhereTheRemoteHasBranch(final @NotNull Path repositoryPath, final @NotNull String remote, final @NotNull String branch) {
        if (!remoteHasBranch(repositoryPath, remote, branch)) {
            Logger.info("Remote " + remote + " has no branch " + branch + " yet; pushing without pulling first");
            return;
        }

        pull(repositoryPath, repositories.getRemoteUrl(repositoryPath, remote), remote, branch);
    }

    // UC-SHARE-016, Rule-SHARE-071
    private void pull(final @NotNull Path repositoryPath, final @NotNull String remoteUrl, final @NotNull String remote, final @NotNull String branch) {
        GitCommandRunner.executeRemote(p, repositoryPath, remoteUrl,
                "git", "pull", "--rebase", "--autostash", remote, branch);
    }

    private boolean remoteHasBranch(final @NotNull Path repositoryPath, final @NotNull String remote, final @NotNull String branch) {
        try {
            return !GitCommandRunner.executeRemote(p, repositoryPath, repositories.getRemoteUrl(repositoryPath, remote),
                    "git", "ls-remote", "--heads", remote, branch).isBlank();
        } catch (final RuntimeException ex) {
            Logger.debug("Could not list " + remote + " branches: " + ex.getMessage());
            return false;
        }
    }

    // UC-SHARE-013, Rule-SHARE-077
    public void push(final @NotNull Path repositoryPath, final @NotNull String remote, final @NotNull String branch) {
        if (remote.isBlank()) throw new IllegalStateException(Bundle.message("git.error.no.remote"));

        GitCommandRunner.executeRemote(p, repositoryPath, repositories.getRemoteUrl(repositoryPath, remote),
                "git", "push", "-u", remote, branch);
        Logger.info("Git push completed for " + repositoryPath);
    }
}
