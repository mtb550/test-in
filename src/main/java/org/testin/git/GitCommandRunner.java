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
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.util.Bundle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class GitCommandRunner {
    static @NotNull String execute(final @NotNull Project p, final @NotNull Path workingDirectory, final @NotNull GitCommand command, final @NotNull String... parameters) {
        return run(p, workingDirectory, "", command, parameters);
    }

    static @NotNull String executeRemote(final @NotNull Project p, final @NotNull Path workingDirectory, final @NotNull String remoteUrl, final @NotNull GitCommand command, final @NotNull String... parameters) {
        return run(p, workingDirectory, remoteUrl, command, parameters);
    }

    // UC-SHARE-012, Rule-SHARE-058
    static void executeOverPaths(final @NotNull Project p, final @NotNull Path workingDirectory, final @NotNull Collection<String> paths, final @NotNull GitCommand command, final @NotNull String... parameters) {
        if (paths.isEmpty()) throw new IllegalArgumentException("Expected paths to run over");

        final @NotNull Path pathspec = writePathspec(paths);
        try {
            final String @NotNull [] full = Arrays.copyOf(parameters, parameters.length + 2);
            full[parameters.length] = "--pathspec-from-file=" + pathspec;
            full[parameters.length + 1] = "--pathspec-file-nul";

            run(p, workingDirectory, "", command, full);
        } finally {
            try {
                Files.deleteIfExists(pathspec);
            } catch (final IOException ex) {
                Logger.warn("Could not delete the pathspec file " + pathspec + ": " + ex.getMessage());
            }
        }
    }

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

    static byte @NotNull [] pathspecBytes(final @NotNull Collection<String> paths) {
        return String.join("\0", paths).getBytes(StandardCharsets.UTF_8);
    }

    // UC-SHARE-012, Rule-SHARE-056
    private static @NotNull String run(final @NotNull Project p, final @NotNull Path workingDirectory, final @NotNull String remoteUrl, final @NotNull GitCommand command, final @NotNull String... parameters) {
        final @NotNull GitLineHandler handler = new GitLineHandler(p, workingDirectory, command);

        handler.addCustomEnvironmentVariable(GitCommand.GIT_EDITOR_ENV, "true");

        handler.addParameters(parameters);
        if (!remoteUrl.isBlank()) handler.setUrl(remoteUrl);

        final @NotNull GitCommandResult result = Git.getInstance().runCommand(handler);
        if (!result.success()) {
            final @NotNull String details = GitSafeText.withoutCredentials(
                    result.getErrorOutputAsJoinedString().isBlank()
                            ? result.getOutputAsJoinedString()
                            : result.getErrorOutputAsJoinedString());

            Logger.error("Git command failed: " + details);
            throw new IllegalStateException(Bundle.message("git.command.failed", details));
        }
        return result.getOutputAsJoinedString();
    }
}
