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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.util.FailureText;
import org.testin.actions.TestinData;
import org.testin.config.TestinYml;
import org.testin.explorer.tree.TreeValues;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.notifications.Notifier;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

public class ViewPendingCommitsAction extends DumbAwareAction {
    // UC-SHARE-010
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.tree(e).flatMap(TreeValues::projectPath).ifPresent(path -> reviewFor(p, path));
    }

    // UC-SHARE-010
    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Rule-SHARE-105
        if (!OptionalPlugin.GIT.enableOrExplain(this, e.getPresentation())) return;

        e.getPresentation().setEnabled(TestinData.firstSelected(e, TestProjectDirectoryDto.class).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    // UC-SHARE-009, Rule-SHARE-042
    public static void reviewFor(final @NotNull Project p, final @NotNull Path path) {
        new Work(p).openFor(path);
    }

    private static @NotNull String commitLabel(final @NotNull String commitId) {
        return commitId.isBlank() ? Bundle.message("git.commit.label.none") : Bundle.message("git.commit.label", commitId);
    }

    private record Work(@NotNull Project p, @NotNull GitRepositoryService git, @NotNull GitCommits commits) {
        private Work(final @NotNull Project p) {
            this(p, new GitRepositoryService(p), new GitCommits(p));
        }

        // UC-SHARE-009, Rule-SHARE-042
        private void openFor(final @NotNull Path path) {
            if (git.isNotRepository(path)) {
                Services.getInstance(p, Notifier.class).warnWithAction(p,
                        Bundle.message("git.no.repository.title"),
                        Bundle.message("git.no.repository.message", path.getFileName()),
                        Bundle.message("git.no.repository.action"),
                        () -> initializeGitRepository(path)
                );

                return;
            }

            scanForChanges(path);
        }

        // UC-SHARE-010, Rule-SHARE-050
        private void scanForChanges(final @NotNull Path path) {
            GitBackgroundTask.run(p, Bundle.message("git.task.scanning"), true,
                    indicator -> {
                        if (git.hasConflicts(path)) {
                            showConflictActions(path, git.getRemoteName(path), git.syncBranch(path), git.conflictingPaths(path));
                            return;
                        }

                        final @NotNull List<PendingChange> changes = GitDiffProcessor.getPendingChanges(p, path);

                        final @NotNull List<String> branches = git.getLocalBranches(path);
                        final @NotNull String current = git.getCurrentBranch(path);

                        final @NotNull OptionalInt unpushed = git.unpushedCount(path);

                        ApplicationManager.getApplication().invokeLater(() ->
                                reviewChanges(path, changes, branches, current, unpushed));
                    },
                    ex -> Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.error.title"), Bundle.message("git.error.diffs", ex.getMessage())));
        }

        // UC-SHARE-010
        private void reviewChanges(final @NotNull Path path, final @NotNull List<PendingChange> changes, final @NotNull List<String> branches, final @NotNull String currentBranch, final @NotNull OptionalInt unpushed) {
            if (changes.isEmpty()) {
                offerThePush(path, currentBranch, unpushed);
                return;
            }

            new PendingCommitsDialog(p, changes, path, branches, currentBranch,
                    request -> commitOnBranch(path, request)).show();
        }

        // UC-SHARE-014, Rule-SHARE-065
        private void commitOnBranch(final @NotNull Path repoPath, final @NotNull PendingCommitsDialog.Request request) {
            final @NotNull String target = request.branch();

            GitBackgroundTask.run(p, Bundle.message("git.task.preparing.branch"), false,
                    indicator -> {
                        final @NotNull String current = git.getCurrentBranch(repoPath);

                        if (target.isEmpty() || target.equals(current)) {
                            performCommitWorkflow(repoPath, request, target.isEmpty() ? current : target);
                            return;
                        }

                        indicator.setText(request.newBranch()
                                ? Bundle.message("git.progress.starting.branch", target)
                                : Bundle.message("git.progress.checking.out", target));

                        final boolean moved = request.newBranch()
                                ? git.startBranch(repoPath, target)
                                : !git.checkout(repoPath, target).isEmpty();

                        // Rule-SHARE-065
                        if (!moved) {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);
                                notifier.errorWithActions(p, Bundle.message("git.branch.not.switched.title"),
                                        Bundle.message("git.branch.not.switched.message", target),
                                        notifier.action(Bundle.message("branch.review.changes"), () -> reviewFor(p, repoPath)));
                            });
                            return;
                        }

                        if (!request.newBranch()) {
                            Services.getInstance(p, ProjectIndexer.class).refreshDirectory(repoPath);
                        }

                        ApplicationManager.getApplication().invokeLater(() -> {
                            final @NotNull TreePanel panel = Services.getInstance(p, TreePanel.class);

                            if (request.newBranch()) panel.refresh();
                            else panel.reindex(Bundle.message("git.switched.to", target));

                            performCommitWorkflow(repoPath, request, target);
                        });
                    },
                    ex -> Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.error.title"),
                            Bundle.message("git.error.prepare", target, ex.getMessage())));
        }

        // UC-SHARE-015, Rule-SHARE-067
        private void offerThePush(final @NotNull Path path, final @NotNull String currentBranch, final @NotNull OptionalInt unpushed) {
            final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);

            if (unpushed.orElse(-1) == 0) {
                notifier.softRefuse(p, Bundle.message("git.no.changes"));
                return;
            }

            final @NotNull String waiting = unpushed.isEmpty()
                    ? Bundle.message("git.not.pushed.no.upstream")
                    : unpushed.orElseThrow() == 1
                            ? Bundle.message("git.not.pushed.one")
                            : Bundle.message("git.not.pushed.many", String.valueOf(unpushed.orElseThrow()));

            notifier.warnWithAction(p, Bundle.message("git.not.pushed.title"),
                    waiting,
                    Bundle.message("git.push.action"),
                    // Rule-SHARE-005
                    () -> pushToRemote(path, () -> commits.headCommitId(path), currentBranch));
        }

        // UC-SHARE-013, Rule-SHARE-059
        private void performCommitWorkflow(final @NotNull Path repoPath, final @NotNull PendingCommitsDialog.Request request, final @NotNull String branch) {
            final @NotNull String commitMessage = request.message();
            final @NotNull Collection<PendingChange> selectedChanges = request.changes();
            final boolean push = request.push();

            GitBackgroundTask.run(p, push ? Bundle.message("git.task.committing.and.pushing") : Bundle.message("git.task.committing"), false,
                    indicator -> {
                        indicator.setText(Bundle.message("git.progress.staging"));
                        commits.stageAndCommit(repoPath, commitMessage, selectedChanges);

                        final @NotNull String commitId = commits.headCommitId(repoPath);

                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (push) {
                                pushToRemote(repoPath, () -> commitId, branch);
                                return;
                            }

                            Services.getInstance(p, Notifier.class).softShow(p, Bundle.message("git.committed"), commitLabel(commitId));
                        });
                    },
                    ex -> {
                        if (isIdentityError(Objects.toString(ex.getMessage(), ""))) {
                            promptAndSetGitIdentity(repoPath, request, branch);
                        } else {
                            Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.commit.failed.title"), Bundle.message("git.commit.failed.message") + System.lineSeparator() + ex.getMessage());
                        }
                    });
        }

        // UC-SHARE-009, Rule-SHARE-043
        private void initializeGitRepository(final @NotNull Path repoPath) {
            GitBackgroundTask.run(p, Bundle.message("git.task.init"), false,
                    indicator -> {
                        git.initialize(repoPath);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Services.getInstance(p, Notifier.class).softShow(p, Bundle.message("git.initialized"));

                            scanForChanges(repoPath);
                        });
                    },
                    ex -> Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.init.failed.title"), Bundle.message("git.init.failed.message", ex.getMessage())));
        }

        // UC-SHARE-013
        private void pushToRemote(final @NotNull Path repoPath, final @NotNull Supplier<@NotNull String> commitToPush, final @NotNull String committedOn) {
            GitBackgroundTask.run(p, Bundle.message("git.task.checking.remote"), false,
                    indicator -> {
                        final @NotNull String commitId = commitToPush.get();
                        final @NotNull String remoteName = git.getRemoteName(repoPath);
                        final @NotNull String remoteUrl = remoteName.isEmpty() ? "" : git.getRemoteUrl(repoPath, remoteName);
                        final @NotNull String branch = committedOn.isBlank() ? git.syncBranch(repoPath) : committedOn;
                        if (branch.isBlank()) {
                            throw new IllegalStateException(Bundle.message("git.error.no.push.branch"));
                        }
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (remoteUrl.isEmpty()) {
                                configureRemoteAndPush(repoPath, remoteName.isEmpty() ? "origin" : remoteName, branch, commitId);
                            } else {
                                executeGitPush(repoPath, remoteName, branch, commitId);
                            }
                        });
                    },
                    ex -> Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.error.title"), Bundle.message("git.error.read.remote", FailureText.of(ex))));
        }

        // UC-SHARE-013, Rule-SHARE-060
        private void configureRemoteAndPush(final @NotNull Path repoPath, final @NotNull String remoteName, final @NotNull String branch, final @NotNull String commitId) {
            final @NotNull Optional<String> known = TestinYml.cloneAddress(p, String.valueOf(repoPath.getFileName()));

            if (known.isPresent()) {
                addRemoteAndPush(repoPath, remoteName, branch, commitId, known.orElseThrow());
                return;
            }

            // Rule-INTERNAL-089
            new RemoteUrlDialog(p, remoteName, typed -> addRemoteAndPush(repoPath, remoteName, branch, commitId, typed)).show();
        }

        // UC-SHARE-013, Rule-SHARE-060
        private void addRemoteAndPush(final @NotNull Path repoPath, final @NotNull String remoteName, final @NotNull String branch, final @NotNull String commitId, final @NotNull String remoteUrl) {
            GitBackgroundTask.run(p, Bundle.message("git.task.configuring.remote"), false,
                    indicator -> {
                        git.configureRemote(repoPath, remoteName, remoteUrl);
                        ApplicationManager.getApplication().invokeLater(() -> executeGitPush(repoPath, remoteName, branch, commitId));
                    },
                    ex -> Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.error.title"), Bundle.message("git.error.add.remote", ex.getMessage())));
        }

        // UC-SHARE-013, Rule-SHARE-061
        private void executeGitPush(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch, final @NotNull String commitId) {
            GitBackgroundTask.run(p, Bundle.message("git.task.pushing.remote"), false,
                    indicator -> {
                        indicator.setText(Bundle.message("git.progress.pull.rebase"));
                        commits.pullAndPush(repoPath, remote, branch);

                        RepositoryRefresh.after(p, repoPath);
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).info(p, Bundle.message("git.pushed.title"),
                                        Bundle.message("git.pushed.message", commitLabel(commitId), remote, branch)));
                    },
                    ex -> {
                        final @NotNull List<String> conflicting = git.conflictingPaths(repoPath);
                        if (!conflicting.isEmpty()) {
                            showConflictActions(repoPath, remote, branch, conflicting);
                            return;
                        }

                        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);
                        notifier.errorWithActions(p, Bundle.message("git.push.failed.title"), FailureText.of(ex),
                                notifier.action(Bundle.message("git.try.again"), () -> pushToRemote(repoPath, () -> commitId, branch)));
                    });
        }

        // UC-SHARE-017
        private void showConflictActions(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch, final @NotNull List<String> conflicting) {
            GitConflictOffer.show(p, conflicting,
                    () -> resolveConflicts(repoPath, remote, branch),
                    () -> finishRebase(repoPath, remote, branch, false),
                    () -> finishRebase(repoPath, remote, branch, true));
        }

        // UC-SHARE-017
        private void resolveConflicts(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch) {
            ApplicationManager.getApplication().executeOnPooledThread(() ->
                    ConflictResolution.resolveRebase(p, repoPath,
                            () -> pushAfterRebase(repoPath, remote, branch),
                            leftOver -> showConflictActions(repoPath, remote, branch, leftOver)));
        }

        // UC-SHARE-017
        private void pushAfterRebase(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch) {
            GitBackgroundTask.run(p, Bundle.message("git.task.pushing.branch", branch), false,
                    indicator -> {
                        commits.push(repoPath, remote, branch);
                        RepositoryRefresh.after(p, repoPath);

                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).info(p, Bundle.message("git.rebase.continued.title"),
                                        Bundle.message("git.rebase.continued.message")));
                    },
                    ex -> Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.push.failed.title"), FailureText.of(ex)));
        }

        // UC-SHARE-017, Rule-SHARE-077
        private void finishRebase(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch, final boolean abort) {
            GitBackgroundTask.run(p, abort ? Bundle.message("git.task.aborting.rebase") : Bundle.message("git.task.continuing.rebase"), false,
                    indicator -> {
                        if (abort) {
                            if (git.couldNotAbortRebase(repoPath))
                                throw new IllegalStateException(Bundle.message("git.error.abort.rebase"));
                        } else {
                            if (git.couldNotContinueRebase(repoPath))
                                throw new IllegalStateException(Bundle.message("git.error.continue.rebase"));
                            commits.push(repoPath, remote, branch);
                        }

                        RepositoryRefresh.after(p, repoPath);

                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).info(p,
                                        abort ? Bundle.message("git.rebase.aborted.title") : Bundle.message("git.rebase.continued.title"),
                                        abort ? Bundle.message("git.rebase.aborted.message") : Bundle.message("git.rebase.continued.message")));
                    },
                    ex -> {
                        final @NotNull List<String> conflicting = git.conflictingPaths(repoPath);
                        if (!conflicting.isEmpty()) showConflictActions(repoPath, remote, branch, conflicting);
                        else
                            Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.conflict.operation.failed.title"), FailureText.of(ex));
                    });
        }

        // UC-SHARE-008, Rule-SHARE-040
        private void promptAndSetGitIdentity(final @NotNull Path repoPath, final @NotNull PendingCommitsDialog.Request request, final @NotNull String branch) {
            ApplicationManager.getApplication().invokeLater(() -> new GitIdentityDialog(p, identity ->
                    GitBackgroundTask.run(p, Bundle.message("git.task.configuring.identity"), false,
                            indicator -> {
                                git.configureIdentity(repoPath, identity.name(), identity.email(), identity.global());
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    Services.getInstance(p, Notifier.class).softShow(p, Bundle.message("git.identity.set"));
                                    performCommitWorkflow(repoPath, request, branch);
                                });
                            },
                            ex -> Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.config.failed.title"),
                                    Bundle.message("git.config.failed.message") + System.lineSeparator() + ex.getMessage()))
            ).show());
        }

        // UC-SHARE-008, Rule-SHARE-039
        private boolean isIdentityError(final @NotNull String message) {
            final @NotNull String normalized = message.toLowerCase(Locale.ROOT);
            return normalized.contains("author identity unknown")
                    || normalized.contains("please tell me who you are");
        }
    }
}
