package org.testin.git;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import org.testin.model.DirectoryType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Local Git repository operations used by the pending-commit workflow.
 */
public final class GitCommitService {

    private final @NotNull Project project;

    /**
     * For the remote's URL, which the network commands need so git4idea can
     * authenticate them. Read through the service that owns that question rather
     * than asked again here - two answers to "what is the remote's URL" is one
     * more than there should be.
     */
    private final @NotNull GitRepositoryService repositories;

    public GitCommitService(final @NotNull Project project) {
        this.project = project;
        this.repositories = new GitRepositoryService(project);
    }

    public void initialize(final @NotNull Path repositoryPath) {
        GitCommandRunner.execute(project, repositoryPath, "git", "init");
    }

    /**
     * Commits the selected test cases, and the marker files that make them mean
     * anything.
     * <p>
     * A directory is a test set because a {@code .ts} sits in it - the indexer
     * decides what every node is by looking for its marker. Committing only the
     * test case JSON therefore pushes files into directories that the colleague
     * who pulls them cannot see as test sets at all, so the cases never appear in
     * their tree. The markers are not shown in the review because there is
     * nothing in them for a tester to review; they simply have to travel.
     */
    public void stageAndCommit(
            final @NotNull Path repositoryPath,
            final @NotNull String message,
            final @NotNull Collection<TestCaseDiff> selectedChanges) {
        final Set<String> paths = GitRefs.repoRelativePaths(selectedChanges);
        if (paths.isEmpty()) throw new IllegalArgumentException("No Git changes were selected");

        paths.addAll(markersAlongside(repositoryPath, paths));

        GitCommandRunner.execute(project, repositoryPath, withPaths(paths, "git", "add", "--"));
        GitCommandRunner.execute(project, repositoryPath, withPaths(paths, "git", "commit", "--only", "-m", message, "--"));
    }

    /**
     * The marker files present in the repository root and in every directory the
     * selected test cases sit under. Checked on disk rather than assumed: which
     * marker a directory carries is what says whether it is a test set, a package
     * or a container, and only one of them is there.
     */
    static @NotNull Set<String> markersAlongside(final @NotNull Path repositoryPath, final @NotNull Set<String> testCasePaths) {
        final Set<String> markers = new LinkedHashSet<>();

        for (final String directory : GitRefs.ancestorDirectories(testCasePaths)) {
            for (final DirectoryType type : DirectoryType.values()) {
                final String marker = type.getMarker();
                if (marker.isBlank()) continue;

                final String relative = directory.isEmpty() ? marker : directory + "/" + marker;
                if (Files.exists(repositoryPath.resolve(relative))) markers.add(relative);
            }
        }
        return markers;
    }

    public void configureRemote(final @NotNull Path repositoryPath, final @NotNull String remoteName, final @NotNull String remoteUrl) {
        GitCommandRunner.execute(project, repositoryPath, "git", "remote", "add", remoteName, remoteUrl);
    }

    public void configureIdentity(
            final @NotNull Path repositoryPath,
            final @NotNull String name,
            final @NotNull String email,
            final boolean global) {
        final String scope = global ? "--global" : "--local";
        GitCommandRunner.execute(project, repositoryPath, "git", "config", scope, "user.name", name);
        GitCommandRunner.execute(project, repositoryPath, "git", "config", scope, "user.email", email);
    }

    /**
     * Rebases on top of the remote and pushes - unless there is nothing there to
     * rebase onto.
     * <p>
     * The pull is what stops a push overwriting a colleague's work, so it is not
     * optional in general. It is impossible on a first push: an empty remote has
     * no branch, and {@code git pull origin master} against one fails with
     * "couldn't find remote ref master" before the push is ever attempted. So the
     * remote is asked whether the branch exists, and the pull is skipped only when
     * it does not.
     */
    public void pullAndPush(final @NotNull Path repositoryPath, final @NotNull String remote, final @NotNull String branch) {
        final String url = repositories.getRemoteUrl(repositoryPath, remote);

        if (remoteHasBranch(repositoryPath, remote, branch)) {
            GitCommandRunner.executeRemote(project, repositoryPath, url, "git", "pull", "--rebase", "--autostash", remote, branch);
        } else {
            Logger.info("Remote " + remote + " has no branch " + branch + " yet; pushing without pulling first");
        }

        push(repositoryPath, remote, branch);
    }

    /**
     * False when the remote has no such branch, and equally when the remote could
     * not be reached - an unreachable remote fails the push a moment later with a
     * message that says so, which is better than failing here with one about a
     * missing branch.
     */
    private boolean remoteHasBranch(final @NotNull Path repositoryPath, final @NotNull String remote, final @NotNull String branch) {
        try {
            return !GitCommandRunner.executeRemote(project, repositoryPath, repositories.getRemoteUrl(repositoryPath, remote),
                    "git", "ls-remote", "--heads", remote, branch).isBlank();
        } catch (final RuntimeException ex) {
            Logger.debug("Could not list " + remote + " branches: " + ex.getMessage());
            return false;
        }
    }



    public void push(final @NotNull Path repositoryPath, final @NotNull String remote, final @NotNull String branch) {
        GitCommandRunner.executeRemote(project, repositoryPath, repositories.getRemoteUrl(repositoryPath, remote),
                "git", "push", "-u", remote, branch);
        Logger.info("Git push completed for " + repositoryPath);
    }

    /**
     * Appends the path set after the fixed arguments. Call sites include the
     * {@code "--"} separator explicitly so the full command stays readable.
     */
    private @NotNull String[] withPaths(final @NotNull Set<String> paths, final @NotNull String... fixedArgs) {
        final String[] result = Arrays.copyOf(fixedArgs, fixedArgs.length + paths.size());
        int index = fixedArgs.length;
        for (final String path : paths) result[index++] = path;
        return result;
    }
}

