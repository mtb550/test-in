package org.testin.git;

import com.intellij.openapi.project.Project;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

/**
 * Small adapter over IntelliJ Git4Idea command execution.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class GitCommandRunner {

    static @NotNull String execute(final @NotNull Project project, final @NotNull Path workingDirectory, final @NotNull String... command) {
        return run(project, workingDirectory, "", command);
    }

    /**
     * Runs a command that talks to a remote, telling the handler which URL it is
     * for.
     * <p>
     * That is what makes {@code GitLineHandler.isRemote()} true, and it is the
     * only reason {@code git4idea} sets up its credential helper for the process. Without
     * it a push over HTTPS gets no authentication support at all: no prompt, no
     * stored credentials, just a failure that reads as a broken plugin. The URL
     * is also how the IDE finds the credentials it already has for that host, so
     * a tester is asked once rather than on every push.
     */
    static @NotNull String executeRemote(final @NotNull Project project, final @NotNull Path workingDirectory, final @NotNull String remoteUrl, final @NotNull String... command) {
        return run(project, workingDirectory, remoteUrl, command);
    }

    /**
     * Runs a command over a set of paths, handing Git the list in a file rather
     * than on the command line.
     * <p>
     * Windows refuses to start a process whose command line exceeds 32,767
     * characters, and it refuses it as {@code CreateProcess error=206}, which
     * names no path and no limit. A test project of 3,320 cases is around 240,000
     * characters of paths, so committing one after an import failed outright -
     * and it failed at the size where a tester has the most to lose.
     * <p>
     * Batching would only raise the ceiling, and cannot be done at all for
     * {@code git commit}: the paths of one commit are one command. A file has no
     * ceiling, so this is the way every path list reaches Git from here on.
     * <p>
     * Separated by NUL, which is the one byte a path cannot contain: the names
     * hold spaces ("Test Cases") and Git applies its own unquoting to a
     * newline-separated list, so a literal separator is the only one that cannot
     * misread a name a tester chose.
     * <p>
     * Needs Git 2.25 or newer, which is where {@code --pathspec-from-file}
     * arrived for both {@code add} and {@code commit}.
     * <p>
     * Answers nothing, unlike its two siblings: the commands that take a path
     * list are {@code add}, which prints nothing when it works, and
     * {@code commit}, whose summary nobody reads. A failure still raises.
     */
    static void executeOverPaths(final @NotNull Project project, final @NotNull Path workingDirectory, final @NotNull Collection<String> paths, final @NotNull String... command) {
        if (paths.isEmpty()) throw new IllegalArgumentException("Expected paths to run over");

        final @NotNull Path pathspec = writePathspec(paths);
        try {
            final String @NotNull [] full = Arrays.copyOf(command, command.length + 2);
            full[command.length] = "--pathspec-from-file=" + pathspec;
            full[command.length + 1] = "--pathspec-file-nul";

            run(project, workingDirectory, "", full);
        } finally {
            try {
                Files.deleteIfExists(pathspec);
            } catch (final IOException ex) {
                Logger.warn("Could not delete the pathspec file " + pathspec + ": " + ex.getMessage());
            }
        }
    }

    /**
     * The bytes Git reads the path list from.
     * <p>
     * Outside the repository deliberately: a file written inside the working
     * tree is an untracked file, and this one exists only while a command that
     * is reading the working tree runs.
     */
    private static @NotNull Path writePathspec(final @NotNull Collection<String> paths) {
        try {
            final @NotNull Path file = Files.createTempFile("testin-pathspec", ".lst");
            Files.write(file, pathspecBytes(paths));
            return file;
        } catch (final IOException ex) {
            Logger.error("Could not write the Git pathspec file: " + ex.getMessage());
            throw new IllegalStateException("Could not write the Git pathspec file: " + ex.getMessage());
        }
    }

    /**
     * The path list as Git reads it: NUL between entries and none after the
     * last, because a trailing separator would leave an empty pathspec behind
     * it and Git rejects the whole command over one.
     * <p>
     * Package-private so a test can read what a tester's paths turn into
     * without starting Git.
     */
    static byte @NotNull [] pathspecBytes(final @NotNull Collection<String> paths) {
        return String.join("\0", paths).getBytes(StandardCharsets.UTF_8);
    }

    private static @NotNull String run(final @NotNull Project project, final @NotNull Path workingDirectory, final @NotNull String remoteUrl, final @NotNull String... command) {
        if (command.length < 2 || !"git".equals(command[0])) {
            throw new IllegalArgumentException("Expected a git command");
        }

        final @NotNull GitCommand gitCommand = commandFor(command[1]);

        final @NotNull GitLineHandler handler = new GitLineHandler(project, workingDirectory, gitCommand);

        // Nothing the plugin runs is a conversation, so no command of it opens an
        // editor. `git rebase --continue` otherwise stops to have the replayed
        // commit's message confirmed - a message the tester never wrote and has
        // nothing to say about - and the sync then waits on a buffer nobody
        // asked for, in the middle of resolving a conflict (#89).
        //
        // Set here rather than at that one call site: every command runs through
        // this method, and the next one that would have opened an editor should
        // not have to remember.
        handler.addCustomEnvironmentVariable(GitCommand.GIT_EDITOR_ENV, "true");

        handler.addParameters(Arrays.copyOfRange(command, 2, command.length));
        if (!remoteUrl.isBlank()) handler.setUrl(remoteUrl);

        final @NotNull GitCommandResult result = Git.getInstance().runCommand(handler);
        if (!result.success()) {
            final @NotNull String details = result.getErrorOutputAsJoinedString().isBlank()
                    ? result.getOutputAsJoinedString()
                    : result.getErrorOutputAsJoinedString();
            Logger.error("Git command failed: " + details);
            throw new IllegalStateException("Git command failed: " + details);
        }
        return result.getOutputAsJoinedString();
    }

    private static @NotNull GitCommand commandFor(final @NotNull String command) {
        return switch (command) {
            case "add" -> GitCommand.ADD;
            case "branch" -> GitCommand.BRANCH;
            case "checkout" -> GitCommand.CHECKOUT;
            case "commit" -> GitCommand.COMMIT;
            case "config" -> GitCommand.CONFIG;
            case "fetch" -> GitCommand.FETCH;
            case "init" -> GitCommand.INIT;
            case "ls-remote" -> GitCommand.LS_REMOTE;
            case "pull" -> GitCommand.PULL;
            case "rebase" -> GitCommand.REBASE;
            case "push" -> GitCommand.PUSH;
            case "remote" -> GitCommand.REMOTE;
            case "rev-list" -> GitCommand.REV_LIST;
            case "rev-parse" -> GitCommand.REV_PARSE;
            case "show" -> GitCommand.SHOW;
            case "status" -> GitCommand.STATUS;
            default -> throw new IllegalArgumentException("Unsupported Git command: " + command);
        };
    }
}
