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

import java.nio.file.Path;
import java.util.Arrays;

/**
 * Small adapter over IntelliJ Git4Idea command execution.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class GitCommandRunner {

    static @NotNull String execute(
            final @NotNull Project project,
            final @NotNull Path workingDirectory,
            final @NotNull String... command) {
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
    static @NotNull String executeRemote(
            final @NotNull Project project,
            final @NotNull Path workingDirectory,
            final @NotNull String remoteUrl,
            final @NotNull String... command) {
        return run(project, workingDirectory, remoteUrl, command);
    }

    private static @NotNull String run(
            final @NotNull Project project,
            final @NotNull Path workingDirectory,
            final @NotNull String remoteUrl,
            final @NotNull String... command) {
        if (command.length < 2 || !"git".equals(command[0])) {
            throw new IllegalArgumentException("Expected a git command");
        }

        final GitCommand gitCommand = commandFor(command[1]);

        final GitLineHandler handler = new GitLineHandler(project, workingDirectory, gitCommand);
        handler.addParameters(Arrays.copyOfRange(command, 2, command.length));
        if (!remoteUrl.isBlank()) handler.setUrl(remoteUrl);

        final GitCommandResult result = Git.getInstance().runCommand(handler);
        if (!result.success()) {
            final String details = result.getErrorOutputAsJoinedString().isBlank()
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
            case "rev-parse" -> GitCommand.REV_PARSE;
            case "show" -> GitCommand.SHOW;
            case "status" -> GitCommand.STATUS;
            default -> throw new IllegalArgumentException("Unsupported Git command: " + command);
        };
    }
}
