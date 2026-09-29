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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinYml;
import org.testin.explorer.TreePanel;
import org.testin.indexer.Nodes;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

record ViewPendingCommitsWork(@NotNull Project p, @NotNull GitRepositoryService git, @NotNull GitCommits commits, @NotNull Notifier notifier, @NotNull Nodes nodes) {
    private static @NotNull String commitLabel(final @NotNull String commitId) {
        return commitId.isBlank() ? Bundle.message("git.commit.label.none") : Bundle.message("git.commit.label", commitId);
    }

    ViewPendingCommitsWork(final @NotNull Project p) {
        this(p, new GitRepositoryService(p), new GitCommits(p), Services.getInstance(p, Notifier.class), Services.getInstance(p, Nodes.class));
    }

    // UC-SHARE-009, Rule-SHARE-042
    void openFor(final @NotNull Path path) {
        if (git.isNotRepository(path)) {
            notifier.warnWithAction(p,
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
                _ -> {
                    final @NotNull Optional<List<String>> unfinished = git.unfinished(path);
                    if (unfinished.isPresent()) {
                        showConflictActions(path, git.getRemoteName(path), git.syncBranch(path), unfinished.orElseThrow());
                        return;
                    }

                    final @NotNull List<PendingChange> changes = GitDiffProcessor.getPendingChanges(p, path);

                    final @NotNull List<String> branches = git.getLocalBranches(path);
                    final @NotNull String current = git.getCurrentBranch(path);

                    final @NotNull OptionalInt unpushed = git.unpushedCount(path);

                    ApplicationManager.getApplication().invokeLater(() ->
                            reviewChanges(path, changes, branches, current, unpushed));
                },
                ex -> notifier.error(p, Bundle.message("git.error.title"), Bundle.message("git.error.diffs", FailureText.of(ex))));
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
    private void commitOnBranch(final @NotNull Path repoPath, final @NotNull Request request) {
        final @NotNull String target = request.branch();

        GitBackgroundTask.run(p, Bundle.message("git.task.preparing.branch"), false,
                indicator -> {
                    final @NotNull String current = git.getCurrentBranch(repoPath);

                    if (target.isEmpty() || target.equals(current)) {
                        performCommitWorkflow(repoPath, request, target.isEmpty() ? current : target);
                        return;
                    }

                    if (request.newBranch()) startBranchThenCommit(repoPath, request, indicator);
                    else checkoutThenCommit(repoPath, request, indicator);
                },
                ex -> notifier.error(p, Bundle.message("git.error.title"),
                        Bundle.message("git.error.prepare", target, FailureText.of(ex))));
    }

    // UC-SHARE-014, Rule-SHARE-065
    private void startBranchThenCommit(final @NotNull Path repoPath, final @NotNull Request request, final @NotNull ProgressIndicator indicator) {
        final @NotNull String target = request.branch();
        indicator.setText(Bundle.message("git.progress.starting.branch", target));

        if (!git.startBranch(repoPath, target)) {
            refuseBranchSwitch(repoPath, target);
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            Services.getInstance(p, TreePanel.class).refresh();
            performCommitWorkflow(repoPath, request, target);
        });
    }

    // UC-SHARE-014, Rule-SHARE-065
    private void checkoutThenCommit(final @NotNull Path repoPath, final @NotNull Request request, final @NotNull ProgressIndicator indicator) {
        final @NotNull String target = request.branch();
        indicator.setText(Bundle.message("git.progress.checking.out", target));

        if (git.checkout(repoPath, target).isEmpty()) {
            refuseBranchSwitch(repoPath, target);
            return;
        }

        nodes.refreshDirectory(repoPath);

        ApplicationManager.getApplication().invokeLater(() -> {
            Services.getInstance(p, TreePanel.class).reindex(Bundle.message("git.switched.to", target));
            performCommitWorkflow(repoPath, request, target);
        });
    }

    // Rule-SHARE-065
    private void refuseBranchSwitch(final @NotNull Path repoPath, final @NotNull String target) {
        ApplicationManager.getApplication().invokeLater(() ->
                notifier.errorWithActions(p, Bundle.message("git.branch.not.switched.title"),
                        Bundle.message("git.branch.not.switched.message", target),
                        notifier.action(Bundle.message("branch.review.changes"), () -> openFor(repoPath))));
    }

    // UC-SHARE-015, Rule-SHARE-067
    private void offerThePush(final @NotNull Path path, final @NotNull String currentBranch, final @NotNull OptionalInt unpushed) {
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
    private void performCommitWorkflow(final @NotNull Path repoPath, final @NotNull Request request, final @NotNull String branch) {
        final @NotNull String commitMessage = request.message();
        final @NotNull Collection<PendingChange> selectedChanges = request.changes();
        final boolean push = request.push();

        GitBackgroundTask.run(p, push ? Bundle.message("git.task.committing.and.pushing") : Bundle.message("git.task.committing"), false,
                indicator -> {
                    if (git.hasNoIdentity(repoPath)) {
                        promptAndSetGitIdentity(repoPath, request, branch);
                        return;
                    }

                    indicator.setText(Bundle.message("git.progress.staging"));
                    commits.stageAndCommit(repoPath, commitMessage, selectedChanges);

                    final @NotNull String commitId = commits.headCommitId(repoPath);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (push) {
                            pushToRemote(repoPath, () -> commitId, branch);
                            return;
                        }

                        notifier.softShow(p, Bundle.message("git.committed"), commitLabel(commitId));
                    });
                },
                ex -> notifier.error(p, Bundle.message("git.commit.failed.title"), Bundle.message("git.commit.failed.message") + System.lineSeparator() + ex.getMessage()));
    }

    // UC-SHARE-009, Rule-SHARE-043
    private void initializeGitRepository(final @NotNull Path repoPath) {
        GitBackgroundTask.run(p, Bundle.message("git.task.init"), false,
                _ -> {
                    git.initialize(repoPath);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        notifier.softShow(p, Bundle.message("git.initialized"));

                        scanForChanges(repoPath);
                    });
                },
                ex -> notifier.error(p, Bundle.message("git.init.failed.title"), Bundle.message("git.init.failed.message", FailureText.of(ex))));
    }

    // UC-SHARE-013
    private void pushToRemote(final @NotNull Path repoPath, final @NotNull Supplier<@NotNull String> commitToPush, final @NotNull String committedOn) {
        GitBackgroundTask.run(p, Bundle.message("git.task.checking.remote"), false,
                _ -> {
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
                            executeGitPush(repoPath, remoteName, remoteUrl, branch, commitId);
                        }
                    });
                },
                ex -> notifier.error(p, Bundle.message("git.error.title"), Bundle.message("git.error.read.remote", FailureText.of(ex))));
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
                _ -> {
                    git.configureRemote(repoPath, remoteName, remoteUrl);
                    ApplicationManager.getApplication().invokeLater(() -> executeGitPush(repoPath, remoteName, remoteUrl, branch, commitId));
                },
                ex -> notifier.error(p, Bundle.message("git.error.title"), Bundle.message("git.error.add.remote", FailureText.of(ex))));
    }

    // UC-SHARE-013, Rule-SHARE-061
    private void executeGitPush(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String remoteUrl, final @NotNull String branch, final @NotNull String commitId) {
        GitBackgroundTask.run(p, Bundle.message("git.task.pushing.remote"), false,
                indicator -> {
                    indicator.setText(Bundle.message("git.progress.pull.rebase"));
                    commits.pullAndPush(repoPath, remote, remoteUrl, branch);

                    RepositoryRefresh.after(p, repoPath);
                    ApplicationManager.getApplication().invokeLater(() ->
                            notifier.info(p, Bundle.message("git.pushed.title"),
                                    Bundle.message("git.pushed.message", commitLabel(commitId), remote, branch)));
                },
                ex -> {
                    final @NotNull List<String> conflicting = git.conflictingPaths(repoPath);
                    if (!conflicting.isEmpty()) {
                        showConflictActions(repoPath, remote, branch, conflicting);
                        return;
                    }

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
                _ -> pushRebased(repoPath, remote, branch),
                ex -> notifier.error(p, Bundle.message("git.push.failed.title"), FailureText.of(ex)));
    }

    // UC-SHARE-017
    private void pushRebased(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch) {
        commits.push(repoPath, remote, branch);
        RepositoryRefresh.after(p, repoPath);

        ApplicationManager.getApplication().invokeLater(() ->
                notifier.info(p, Bundle.message("git.rebase.continued.title"),
                        Bundle.message("git.rebase.continued.message")));
    }

    // UC-SHARE-017, Rule-SHARE-077
    private void continueRebase(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch) {
        if (git.couldNotContinueRebase(repoPath))
            throw new IllegalStateException(Bundle.message("git.error.continue.rebase"));

        pushRebased(repoPath, remote, branch);
    }

    // UC-SHARE-017, Rule-SHARE-077
    private void abortRebase(final @NotNull Path repoPath) {
        if (git.couldNotAbortRebase(repoPath))
            throw new IllegalStateException(Bundle.message("git.error.abort.rebase"));

        RepositoryRefresh.after(p, repoPath);

        ApplicationManager.getApplication().invokeLater(() ->
                notifier.info(p, Bundle.message("git.rebase.aborted.title"), Bundle.message("git.rebase.aborted.message")));
    }

    // UC-SHARE-017, Rule-SHARE-077
    private void finishRebase(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch, final boolean abort) {
        GitBackgroundTask.run(p, abort ? Bundle.message("git.task.aborting.rebase") : Bundle.message("git.task.continuing.rebase"), false,
                _ -> {
                    if (abort) abortRebase(repoPath);
                    else continueRebase(repoPath, remote, branch);
                },
                ex -> {
                    final @NotNull List<String> conflicting = git.conflictingPaths(repoPath);
                    if (!conflicting.isEmpty()) showConflictActions(repoPath, remote, branch, conflicting);
                    else
                        notifier.error(p, Bundle.message("git.conflict.operation.failed.title"), FailureText.of(ex));
                });
    }

    // UC-SHARE-008, Rule-SHARE-040
    private void promptAndSetGitIdentity(final @NotNull Path repoPath, final @NotNull Request request, final @NotNull String branch) {
        ApplicationManager.getApplication().invokeLater(() -> new GitIdentityDialog(p, identity ->
                GitBackgroundTask.run(p, Bundle.message("git.task.configuring.identity"), false,
                        _ -> {
                            git.configureIdentity(repoPath, identity.name(), identity.email(), identity.global());
                            ApplicationManager.getApplication().invokeLater(() -> {
                                notifier.softShow(p, Bundle.message("git.identity.set"));
                                performCommitWorkflow(repoPath, request, branch);
                            });
                        },
                        ex -> notifier.error(p, Bundle.message("git.config.failed.title"),
                                Bundle.message("git.config.failed.message") + System.lineSeparator() + ex.getMessage()))
        ).show());
    }
}
