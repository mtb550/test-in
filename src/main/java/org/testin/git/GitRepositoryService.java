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
import git4idea.GitUtil;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.services.OptionalPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.Optional;
import java.util.List;

@AllArgsConstructor
public final class GitRepositoryService {
    private final @NotNull Project p;

        // UC-SHARE-009
    public void initialize(final @NotNull Path repositoryPath) {
        GitCommandRunner.execute(p, repositoryPath, "git", "init");
    }

    // UC-SHARE-013
    public void configureRemote(final @NotNull Path repositoryPath, final @NotNull String remoteName, final @NotNull String remoteUrl) {
        GitCommandRunner.execute(p, repositoryPath, "git", "remote", "add", remoteName, remoteUrl);
    }

    // UC-SHARE-008, Rule-SHARE-041
    public void configureIdentity(final @NotNull Path repositoryPath, final @NotNull String name, final @NotNull String email, final boolean global) {
        final @NotNull String scope = global ? "--global" : "--local";
        GitCommandRunner.execute(p, repositoryPath, "git", "config", scope, "user.name", name);
        GitCommandRunner.execute(p, repositoryPath, "git", "config", scope, "user.email", email);
    }

// Rule-TREE-PANEL-104
    public boolean isNotRepository(final @NotNull Path path) {
        return !OptionalPlugin.GIT.isAvailable() || !GitUtil.isGitRoot(path);
    }

    public @NotNull String getCurrentBranch(final @NotNull Path path) {
        return run(path, "git", "branch", "--show-current").orElse("").trim();
    }

    public @NotNull String getDefaultBranch(final @NotNull Path path) {
        final @NotNull String currentBranch = getCurrentBranch(path);
        final @NotNull String remoteName = getRemoteName(path);
        if (remoteName.isEmpty()) return currentBranch;

        final @NotNull String headBranch = runRemote(path, getRemoteUrl(path, remoteName), "git", "remote", "show", remoteName)
                .map(GitRefs::parseHeadBranch)
                .orElse("");

        return headBranch.isEmpty() ? currentBranch : headBranch;
    }

    // UC-SHARE-016, Rule-SHARE-068
    public @NotNull String syncBranch(final @NotNull Path path) {
        final @NotNull String current = getCurrentBranch(path);

        return current.isEmpty() ? getDefaultBranch(path) : current;
    }

    public int rebaseStep(final @NotNull Path path) {
        final @NotNull Path gitDir = path.resolve(".git");

        return readStep(gitDir.resolve("rebase-merge").resolve("msgnum"))
                + readStep(gitDir.resolve("rebase-apply").resolve("next"));
    }

    private int readStep(final @NotNull Path counter) {
        try {
            return Integer.parseInt(Files.readString(counter).trim());
        } catch (final IOException | NumberFormatException ex) {
            return 0;
        }
    }

    // UC-TREE-PANEL-026
    public void fetchRemoteBranches(final @NotNull Path path) {
        final @NotNull String remoteName = getRemoteName(path);
        if (remoteName.isEmpty()) return;

        GitCommandRunner.executeRemote(p, path, getRemoteUrl(path, remoteName), "git", "fetch", "--all", "--prune");
    }

    // UC-TREE-PANEL-026
    public @NotNull List<String> getAvailableBranches(final @NotNull Path path) {
        return GitRefs.parseBranches(GitCommandRunner.execute(p, path, "git", "branch", "-a").lines().toList());
    }

    public @NotNull String getRemoteUrl(final @NotNull Path path, final @NotNull String remoteName) {
        return run(path, "git", "remote", "get-url", remoteName).orElse("").trim();
    }

    public @NotNull String getRemoteName(final @NotNull Path path) {
        return GitRefs.chooseRemote(run(path, "git", "remote").orElse("").lines().toList());
    }

    public @NotNull String remoteUrl(final @NotNull Path path) {
        final @NotNull String remoteName = getRemoteName(path);
        return remoteName.isEmpty() ? "" : getRemoteUrl(path, remoteName);
    }

    public @NotNull String checkout(final @NotNull Path path, final @NotNull String branch) {
        final @NotNull List<String> localBranches = getLocalBranches(path);
        final boolean remoteBranch = GitRefs.isRemoteBranch(branch, localBranches, run(path, "git", "remote").orElse("").lines().toList());
        final @NotNull String target = remoteBranch ? GitRefs.localNameOf(branch) : branch;

        if (remoteBranch && !localBranches.contains(target)) {
            return run(path, "git", "checkout", "-b", target, "--track", branch).isPresent() ? target : "";
        }

        return run(path, "git", "checkout", target).isPresent() ? target : "";
    }

    // UC-SHARE-014, Rule-SHARE-064
    public boolean startBranch(final @NotNull Path path, final @NotNull String branch) {
        return run(path, "git", "checkout", "-b", branch).isPresent();
    }

    // UC-SHARE-014, Rule-SHARE-063
    public @NotNull List<String> getLocalBranches(final @NotNull Path path) {
        return GitRefs.parseBranches(run(path, "git", "branch").orElse("").lines().toList());
    }

    // UC-SHARE-010, Rule-SHARE-044
    public @NotNull List<String> status(final @NotNull Path path) {
        return run(path, "git", "status", "--porcelain", "-uall").orElse("")
                .lines().filter(line -> !line.isBlank()).toList();
    }

    public @NotNull String showAtHead(final @NotNull Path path, final @NotNull String relativePath) {
        return run(path, "git", "show", "HEAD:" + relativePath).orElse("");
    }

    public boolean hasConflicts(final @NotNull Path path) {
        if (isRebaseInProgress(path)) return true;

        return GitRefs.hasUnmergedPaths(run(path, "git", "status", "--porcelain").orElse("").lines().toList());
    }

    public @NotNull String stageContent(final @NotNull Path path, final @NotNull String relativePath, final int stage) {
        return run(path, "git", "show", ":" + stage + ":" + relativePath).orElse("");
    }

    public boolean stageResolved(final @NotNull Path path, final @NotNull String relativePath) {
        return run(path, "git", "add", "--", relativePath).isPresent();
    }

    public @NotNull List<String> conflictingPaths(final @NotNull Path path) {
        return GitRefs.unmergedPaths(status(path));
    }

    // UC-SHARE-015, Rule-SHARE-066
    public @NotNull OptionalInt unpushedCount(final @NotNull Path path) {
        final @NotNull String counted = run(path, "git", "rev-list", "--count", "@{upstream}..HEAD").orElse("").trim();
        if (counted.isEmpty()) return OptionalInt.empty();

        try {
            return OptionalInt.of(Integer.parseInt(counted));
        } catch (final NumberFormatException ex) {
            Logger.debug("Could not read the unpushed count in " + path + ": " + counted);
            return OptionalInt.empty();
        }
    }

    private boolean isRebaseInProgress(final @NotNull Path path) {
        final @NotNull Path gitDir = path.resolve(".git");
        return Files.isDirectory(gitDir.resolve("rebase-merge")) || Files.isDirectory(gitDir.resolve("rebase-apply"));
    }

    public boolean couldNotAbortRebase(final @NotNull Path path) {
        return run(path, "git", "rebase", "--abort").isEmpty();
    }

    public boolean couldNotContinueRebase(final @NotNull Path path) {
        return run(path, "git", "rebase", "--continue").isEmpty();
    }

    private @NotNull Optional<String> run(final @NotNull Path path, final @NotNull String... command) {
        return execute(path, "", command);
    }

    private @NotNull Optional<String> runRemote(final @NotNull Path path, final @NotNull String remoteUrl, final @NotNull String... command) {
        return execute(path, remoteUrl, command);
    }

    private @NotNull Optional<String> execute(final @NotNull Path path, final @NotNull String remoteUrl, final @NotNull String... command) {
        try {
            return Optional.of(GitCommandRunner.executeRemote(p, path, remoteUrl, command));
        } catch (final RuntimeException ex) {
            Logger.debug("git " + GitSafeText.withoutCredentials(String.join(" ", command))
                    + " failed in " + path + ": " + ex.getMessage());
            return Optional.empty();
        }
    }
}
