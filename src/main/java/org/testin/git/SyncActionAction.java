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
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.explorer.tree.TreeValues;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class SyncActionAction extends DumbAwareAction {
    private static @NotNull Optional<Path> activeProjectPath(final @NotNull AnActionEvent e) {
        return TestinData.tree(e).flatMap(SyncActionAction::activeProjectIn);
    }

    private static @NotNull Optional<Path> activeProjectIn(final @NotNull SimpleTree tree) {
        return Optional.ofNullable(tree.getSelectionPath())
                .flatMap(SyncActionAction::projectOn)
                .or(() -> TreeValues.projectPath(tree));
    }

    private static @NotNull Optional<Path> projectOn(final @NotNull TreePath selectionPath) {
        for (final Object component : selectionPath.getPath()) {
            final @NotNull Optional<Path> project = TreeValues.valueOf(component, TestProjectDirectoryDto.class)
                    .map(TestProjectDirectoryDto::getPath);
            if (project.isPresent()) return project;
        }

        return Optional.empty();
    }

    // UC-SHARE-016
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        activeProjectPath(e).ifPresentOrElse(path -> new Work(p).syncRepository(path), () ->
                Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("git.sync.error.title"),
                        Bundle.message("git.sync.no.project")));
    }

    // UC-SHARE-016
    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Rule-SHARE-105
        if (OptionalPlugin.GIT.grayedWithReason(this, e.getPresentation())) return;

        e.getPresentation().setEnabled(TestinData.firstSelected(e, TestProjectDirectoryDto.class).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private record Work(@NotNull Project p, @NotNull GitRepositoryService git, @NotNull GitCommits commits) {
        private Work(final @NotNull Project p) {
            this(p, new GitRepositoryService(p), new GitCommits(p));
        }

        // UC-SHARE-016, Rule-SHARE-070
        private static @NotNull String pushedMessage(final @NotNull OptionalInt pushed) {
            if (pushed.isEmpty()) return Bundle.message("git.synced.pushed.upstream");

            if (pushed.getAsInt() == 0) return Bundle.message("git.synced.up.to.date");

            return pushed.getAsInt() == 1
                    ? Bundle.message("git.synced.pushed.one")
                    : Bundle.message("git.synced.pushed.many", String.valueOf(pushed.getAsInt()));
        }

        // UC-SHARE-016, Rule-SHARE-069
        private void syncRepository(final @NotNull Path repoPath) {
            if (git.isNotRepository(repoPath)) {
                Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("git.sync.nothing.title"),
                        Bundle.message("git.sync.nothing.message", repoPath.getFileName()));
                return;
            }

            // Rule-SHARE-005
            // Rule-SHARE-072
            GitBackgroundTask.run(p, Bundle.message("git.task.syncing"), false,
                    indicator -> {
                        indicator.setText(Bundle.message("git.progress.checking.remote"));
                        final @NotNull String remoteName = git.getRemoteName(repoPath);
                        final @NotNull String remoteUrl = remoteName.isEmpty() ? "" : git.getRemoteUrl(repoPath, remoteName);

                        if (remoteUrl.isEmpty()) {
                            ApplicationManager.getApplication().invokeLater(() ->
                                    Services.getInstance(p, Notifier.class).warn(p, Bundle.message("git.sync.aborted.title"), Bundle.message("git.sync.aborted.message"))
                            );
                            return;
                        }

                        final @NotNull String branch = git.syncBranch(repoPath);
                        if (branch.isBlank()) {
                            throw new IllegalStateException(Bundle.message("git.error.no.sync.branch"));
                        }

                        if (git.hasConflicts(repoPath)) {
                            final @NotNull List<String> unfinished = git.conflictingPaths(repoPath);
                            ApplicationManager.getApplication().invokeLater(() -> showConflictActions(repoPath, unfinished));
                            return;
                        }

                        indicator.setText(Bundle.message("git.progress.pulling", branch));
                        commits.pullWhereTheRemoteHasBranch(repoPath, remoteName, branch);

                        indicator.setText(Bundle.message("git.progress.pushing.committed"));
                        final @NotNull OptionalInt pushed = pushUnpushed(repoPath, remoteName, branch);

                        indicator.setText(Bundle.message("git.progress.refreshing"));
                        refreshAfterSync(repoPath, pushed);

                    },
                    ex -> {
                        Logger.error(FailureText.of(ex));

                        final @NotNull List<String> conflicting = git.conflictingPaths(repoPath);

                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (!conflicting.isEmpty()) {
                                showConflictActions(repoPath, conflicting);
                            } else {
                                reportSyncFailure(Bundle.message("git.sync.failed.remote", ex.getMessage()));
                            }
                        });
                    });
        }

        private void showConflictActions(final @NotNull Path repoPath, final @NotNull List<String> conflicting) {
            GitConflictOffer.show(p, conflicting,
                    () -> resolveConflicts(repoPath),
                    () -> finishRebase(repoPath, false),
                    () -> finishRebase(repoPath, true));
        }

        // UC-SHARE-017
        private void resolveConflicts(final @NotNull Path repoPath) {
            ApplicationManager.getApplication().executeOnPooledThread(() ->
                    ConflictResolution.resolveRebase(p, repoPath,
                            () -> finishSyncInBackground(repoPath),
                            leftOver -> showConflictActions(repoPath, leftOver)));
        }

        private void reportRebaseFailure(final @NotNull Path repoPath, final @NotNull String message) {
            final @NotNull List<String> conflicting = git.conflictingPaths(repoPath);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (!conflicting.isEmpty()) showConflictActions(repoPath, conflicting);
                else
                    Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.conflict.operation.failed.title"), message);
            });
        }

        private void reportSyncFailure(final @NotNull String detail) {
            Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.sync.failed.title"), detail);
        }

        // UC-SHARE-017, Rule-SHARE-077
        private void finishRebase(final @NotNull Path repoPath, final boolean abort) {
            final @NotNull String failure = abort ? Bundle.message("git.error.abort.rebase") : Bundle.message("git.error.continue.rebase");

            GitBackgroundTask.run(p, abort ? Bundle.message("git.task.aborting.rebase") : Bundle.message("git.task.continuing.rebase"), false,
                    indicator -> {
                        if (abort) {
                            if (git.couldNotAbortRebase(repoPath)) {
                                reportRebaseFailure(repoPath, failure);
                                return;
                            }
                            refreshRepository(repoPath);
                            ApplicationManager.getApplication().invokeLater(() ->
                                    Services.getInstance(p, Notifier.class).info(p, Bundle.message("git.rebase.aborted.title"), Bundle.message("git.rebase.aborted.pull.message")));
                            return;
                        }

                        if (git.couldNotContinueRebase(repoPath)) {
                            reportRebaseFailure(repoPath, failure);
                            return;
                        }

                        finishSyncInBackground(repoPath);
                    },
                    ex -> {
                        Logger.error(FailureText.of(ex));
                        reportRebaseFailure(repoPath, failure);
                    });
        }

        private void finishSyncInBackground(final @NotNull Path repoPath) {
            GitBackgroundTask.run(p, Bundle.message("git.task.finishing.sync"), false,
                    indicator -> {
                        final @NotNull OptionalInt pushed;
                        try {
                            indicator.setText(Bundle.message("git.progress.pushing.committed"));
                            pushed = pushUnpushed(repoPath, git.getRemoteName(repoPath), git.syncBranch(repoPath));
                        } catch (final Exception ex) {
                            Logger.error("Could not push after resolving: " + ex.getMessage());
                            ApplicationManager.getApplication().invokeLater(() ->
                                    Services.getInstance(p, Notifier.class).error(p, Bundle.message("git.push.failed.title"),
                                            Bundle.message("git.push.failed.after.resolve", ex.getMessage())));

                            indicator.setText(Bundle.message("git.progress.refreshing"));
                            refreshRepository(repoPath);
                            return;
                        }

                        indicator.setText(Bundle.message("git.progress.refreshing"));
                        refreshAfterSync(repoPath, pushed);
                    },
                    ex -> {
                        Logger.error(FailureText.of(ex));
                        ApplicationManager.getApplication().invokeLater(() ->
                                reportSyncFailure(Bundle.message("git.sync.did.not.finish", ex.getMessage())));
                    });
        }

        // UC-SHARE-016, Rule-SHARE-070
        private @NotNull OptionalInt pushUnpushed(final @NotNull Path repoPath, final @NotNull String remote, final @NotNull String branch) {
            final @NotNull OptionalInt unpushed = git.unpushedCount(repoPath);
            if (unpushed.orElse(-1) == 0) return OptionalInt.of(0);

            commits.push(repoPath, remote, branch);
            return unpushed;
        }

        // UC-SHARE-016
        private void refreshAfterSync(final @NotNull Path repoPath, final @NotNull OptionalInt pushed) {
            RepositoryRefresh.after(p, repoPath);
            ApplicationManager.getApplication().invokeLater(() ->
                    Services.getInstance(p, Notifier.class).info(p, Bundle.message("git.synced.title"), pushedMessage(pushed)));
        }

        private void refreshRepository(final @NotNull Path repoPath) {
            RepositoryRefresh.after(p, repoPath);
        }
    }
}
