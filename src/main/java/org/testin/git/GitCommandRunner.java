package org.testin.git;

import com.intellij.openapi.project.Project;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * Small adapter over IntelliJ Git4Idea command execution.
 */
final class GitCommandRunner {

    private GitCommandRunner() {
    }

    static @NotNull String execute(
            final @NotNull Project project,
            final @NotNull Path workingDirectory,
            final @NotNull String... command) {
        if (command.length < 2 || !"git".equals(command[0])) {
            throw new IllegalArgumentException("Expected a git command");
        }

        final GitCommand gitCommand = commandFor(command[1]);

        final GitLineHandler handler = new GitLineHandler(project, workingDirectory, gitCommand);
        handler.addParameters(Arrays.copyOfRange(command, 2, command.length));

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
            case "pull" -> GitCommand.PULL;
            case "push" -> GitCommand.PUSH;
            case "remote" -> GitCommand.REMOTE;
            default -> throw new IllegalArgumentException("Unsupported Git command: " + command);
        };
    }
}
