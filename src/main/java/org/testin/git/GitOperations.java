package org.testin.git;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Every Git capability Testin uses, free of Git4Idea types so callers load in
 * IDEs without the Git plugin. The Git4Idea-backed implementation is registered
 * as a project service in {@code META-INF/testin-git.xml}, which the platform
 * reads only when Git4Idea is present — so
 * {@code Services.getInstance(p, GitOperations.class)} returns {@code null}
 * exactly when {@code OptionalPlugin.GIT} is unavailable.
 *
 * <p>Methods throw {@link GitOperationException} on failure unless stated
 * otherwise.</p>
 */
public interface GitOperations {

    // ------------------------------------------------------------------
    // Repository and branch queries
    // ------------------------------------------------------------------

    boolean isRepository(@NotNull Path path);

    @Nullable
    String getCurrentBranch(@NotNull Path path);

    /**
     * The remote HEAD branch when a remote is configured, otherwise the
     * current branch.
     */
    @Nullable
    String getDefaultBranch(@NotNull Path path);

    /**
     * Local and remote branch names, sorted, without {@code HEAD} refs.
     */
    @NotNull
    List<String> getAvailableBranches(@NotNull Path path);

    /**
     * No-op when the repository has no remote.
     */
    void fetchRemoteBranches(@NotNull Path path);

    /**
     * The remote to sync with: {@code origin} when present, otherwise the
     * first configured remote, otherwise {@code null}.
     */
    @Nullable
    String getRemoteName(@NotNull Path path);

    /**
     * Empty string when the remote has no URL configured.
     */
    @NotNull
    String getRemoteUrl(@NotNull Path path, @NotNull String remoteName);

    // ------------------------------------------------------------------
    // Branch and sync operations
    // ------------------------------------------------------------------

    /**
     * Checks out {@code branch}, creating a tracking local branch when a
     * remote branch name is given. Returns the resulting local branch name.
     */
    @NotNull
    String checkout(@NotNull Path path, @NotNull String branch);

    boolean hasConflicts(@NotNull Path path);

    void abortRebase(@NotNull Path path);

    void continueRebase(@NotNull Path path);

    /**
     * {@code pull --rebase --autostash} from the given remote branch.
     */
    void pull(@NotNull Path path, @NotNull String remote, @NotNull String branch);

    // ------------------------------------------------------------------
    // Commit workflow
    // ------------------------------------------------------------------

    void initialize(@NotNull Path path);

    /**
     * Stages and commits only the files behind the selected changes.
     */
    void stageAndCommit(@NotNull Path path, @NotNull String message, @NotNull Collection<TestCaseDiff> selectedChanges);

    void configureRemote(@NotNull Path path, @NotNull String remoteName, @NotNull String remoteUrl);

    void configureIdentity(@NotNull Path path, @NotNull String name, @NotNull String email, boolean global);

    void pullAndPush(@NotNull Path path, @NotNull String remote, @NotNull String branch);

    void push(@NotNull Path path, @NotNull String remote, @NotNull String branch);

    // ------------------------------------------------------------------
    // Project lifecycle
    // ------------------------------------------------------------------

    void cloneRepository(@NotNull Path parentDirectory, @NotNull String url, @NotNull String directoryName);

    /**
     * Refreshes the VFS under the repository root after external changes.
     */
    void refreshRoot(@NotNull Path path);
}
