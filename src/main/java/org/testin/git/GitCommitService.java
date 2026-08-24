package org.testin.git;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Local Git repository operations used by the pending-commit workflow.
 */
public final class GitCommitService {

    private final @NotNull Project project;

    /**
     * For the remote's URL, which the network commands need so {@code git4idea} can
     * authenticate them. Read through the service that owns that question rather
     * than asked again here - two answers to "what is the remote's URL" is one
     * more than there should be.
     */
    private final @NotNull GitRepositoryService repositories;

    public GitCommitService(final @NotNull Project project) {
        this.project = project;
        this.repositories = new GitRepositoryService(project);
    }

    /**
     * The marker files present in the repository root and in every directory the
     * selected test cases sit under. Checked on disk rather than assumed: which
     * marker a directory carries is what says whether it is a test set, a package
     * or a container, and only one of them is there.
     */
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

    public void initialize(final @NotNull Path repositoryPath) {
        GitCommandRunner.execute(project, repositoryPath, "git", "init");
    }

    /**
     * Commits what the tester selected, and the marker files that make it mean
     * anything.
     * <p>
     * A directory is a test set because a {@code .ts} sits in it - the indexer
     * decides what every node is by looking for its marker. Committing only the
     * test case JSON therefore pushes files into directories that the colleague
     * who pulls them cannot see as test sets at all, so the cases never appear in
     * their tree.
     * <p>
     * The review lists markers in their own right now, so one can be committed
     * deliberately: archiving a project is a marker edit and nothing else. The
     * ones above a selected case travel whether they were picked or not, because
     * without them the case lands somewhere nothing recognizes.
     */
    public void stageAndCommit(final @NotNull Path repositoryPath, final @NotNull String message, final @NotNull Collection<PendingChange> selectedChanges) {
        final @NotNull Set<String> paths = GitRefs.repoRelativePaths(selectedChanges);
        if (paths.isEmpty()) throw new IllegalArgumentException("No Git changes were selected");

        paths.addAll(markersAlongside(repositoryPath, paths));

        final @NotNull Set<String> stageable = stageable(repositoryPath, paths);
        if (!stageable.isEmpty()) {
            GitCommandRunner.executeOverPaths(project, repositoryPath, stageable, "git", "add");
        }

        GitCommandRunner.executeOverPaths(project, repositoryPath, paths, "git", "commit", "--only", "-m", message);
    }

    /**
     * Of the paths to commit, the ones {@code git add} can be given.
     * <p>
     * It refuses a path that is in neither the working tree nor the index, and
     * one such path fails the whole command - so the commit would not happen at
     * all. The old side of a rename someone staged in another tool is exactly
     * that path: the index already carries the move, and nothing by that name is
     * left on disk.
     * <p>
     * Nothing is lost by leaving them out. {@code git commit --only} takes the
     * working tree for every path it is given, so a file that is gone is
     * committed as the deletion it is, staged or not. What still needs staging is
     * the untracked file, which is every new test case.
     * <p>
     * Static and package-private so the Git workflow test stages the way the
     * plugin does, instead of keeping a second copy of this rule that cannot be
     * wrong in the same way.
     */
    static @NotNull Set<String> stageable(final @NotNull Path repositoryPath, final @NotNull Set<String> paths) {
        return paths.stream()
                .filter(path -> Files.exists(repositoryPath.resolve(path)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * The short id of the commit at HEAD - what a tester quotes when they say
     * which commit their work went into, and what the push reports afterward.
     * <p>
     * Blank rather than a failure when Git cannot answer: a repository with no
     * commit in it yet has no HEAD, and the commit that just succeeded must not
     * be reported as failed because the label for it could not be read.
     */
    public @NotNull String headCommitId(final @NotNull Path repositoryPath) {
        try {
            return GitCommandRunner.execute(project, repositoryPath, "git", "rev-parse", "--short", "HEAD").trim();
        } catch (final RuntimeException ex) {
            Logger.warn("Could not read the commit id: " + ex.getMessage());
            return "";
        }
    }

    public void configureRemote(final @NotNull Path repositoryPath, final @NotNull String remoteName, final @NotNull String remoteUrl) {
        GitCommandRunner.execute(project, repositoryPath, "git", "remote", "add", remoteName, remoteUrl);
    }

    public void configureIdentity(final @NotNull Path repositoryPath, final @NotNull String name, final @NotNull String email, final boolean global) {
        final @NotNull String scope = global ? "--global" : "--local";
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
        final @NotNull String url = repositories.getRemoteUrl(repositoryPath, remote);

        if (remoteHasBranch(repositoryPath, remote, branch)) {
            GitCommandRunner.executeRemote(project, repositoryPath, url, "git", "pull", "--rebase", "--autostash", remote, branch);
        } else {
            Logger.info("Remote " + remote + " has no branch " + branch + " yet; pushing without pulling first");
        }

        push(repositoryPath, remote, branch);
    }

    /**
     * False when the remote has no such branch, and equally when the remote could
     * not be reached.
     * <p>
     * An unreachable remote fails the push a moment later, with a message that
     * says so. That is better than failing here with one about a missing branch.
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

}

