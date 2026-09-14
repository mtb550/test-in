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

package org.testin.bug;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.io.FileUtil;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.config.BugRepository;
import org.testin.logger.Logger;
import org.testin.util.Bundle;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The one place Testin talks to the GitHub CLI (#28): whether a bug can be
 * sent, and sending it.
 * <p>
 * {@code gh} holds the sign-in, so Testin stores no credential and speaks no
 * HTTP. It is looked up on the IDE's PATH, with no setting for where it lives.
 * An IDE started from JetBrains Toolbox sees Toolbox's PATH, which is why the
 * reason for a missing {@code gh} says to restart both.
 */
@AllArgsConstructor
public final class GitHubCli {

    /**
     * The first {@code gh} that attaches files to a new issue.
     */
    private static final @NotNull List<Integer> OLDEST = List.of(2, 99, 0);

    /**
     * How long {@code gh} is given before it is stopped: long enough for fifty
     * screenshots on a slow line, and short enough that a dead one ends.
     */
    static final @NotNull Duration TIMEOUT = Duration.ofSeconds(120);

    private static final @NotNull String BODY_FILE = "bug.md";
    private static final @NotNull String DEV_BUILD = "DEV";
    private static final @NotNull Pattern VERSION = Pattern.compile("gh version (\\S+)");
    private static final @NotNull Pattern NUMBER = Pattern.compile("\\d{1,9}");

    /**
     * Where the readiness questions run from. They write nothing, so any folder
     * that exists will do.
     */
    private static final @NotNull Path ANYWHERE = Path.of(System.getProperty("java.io.tmpdir"));

    /**
     * How {@code gh} is started. A test hands in its own, so no test ever starts
     * one.
     */
    @FunctionalInterface
    interface Launcher {

        /**
         * What {@code gh} answered, and empty when there is no {@code gh} to ask.
         */
        @NotNull Optional<ProcessOutput> run(@NotNull List<String> arguments, @NotNull Path workDirectory);
    }

    private final @NotNull Launcher launcher;

    /**
     * The {@code gh} on the IDE's PATH, run under this indicator: where the
     * indicator can be canceled, Cancel stops {@code gh}.
     */
    public static @NotNull GitHubCli onPath(final @NotNull ProgressIndicator indicator) {
        return new GitHubCli((arguments, workDirectory) -> start(arguments, workDirectory, indicator));
    }

    /**
     * UC-VIEW-PANEL-016, Rule-VIEW-PANEL-071, Rule-VIEW-PANEL-078.
     * <p>
     * Why a bug cannot be sent, stopping at the first thing that fails, and
     * empty when it can: {@code bugRepoUrl} missing or not a repository address,
     * no {@code gh}, a {@code gh} too old to attach files, not signed in to the
     * repository's host. A {@code bugRepoUrl} that fails never starts
     * {@code gh}.
     * <p>
     * Off the EDT.
     */
    public @NotNull Optional<String> whyItCannotSend(final @NotNull String bugRepoUrl) {
        if (bugRepoUrl.isBlank()) return Optional.of(Bundle.message("bug.reason.no.bug.repo.url"));

        return BugRepository.of(bugRepoUrl)
                .map(this::whyGhCannotSend)
                .orElseGet(() -> Optional.of(Bundle.message("bug.reason.not.a.repository")));
    }

    private @NotNull Optional<String> whyGhCannotSend(final @NotNull BugRepository repository) {
        final @NotNull Optional<String> version = launcher.run(List.of("--version"), ANYWHERE).map(ProcessOutput::getStdout);
        if (version.isEmpty()) return Optional.of(Bundle.message("bug.reason.no.gh"));

        final @NotNull String said = version.orElse("");
        if (!isNewEnough(said)) return Optional.of(Bundle.message("bug.reason.old.gh", versionIn(said), oldest()));

        final boolean signedIn = launcher.run(List.of("auth", "status", "--active", "--hostname", repository.host()), ANYWHERE)
                .filter(answer -> answer.getExitCode() == 0 && !answer.isTimeout() && !answer.isCancelled())
                .isPresent();
        return signedIn ? Optional.empty() : Optional.of(Bundle.message("bug.reason.signed.out", repository.host()));
    }

    /**
     * UC-VIEW-PANEL-016, Rule-VIEW-PANEL-073, Rule-VIEW-PANEL-076.
     * <p>
     * Files the issue with the title and body the tester left, the screenshots
     * attached.
     * <p>
     * The body and the screenshots are written into a fresh temporary folder
     * that {@code gh} runs from, and the folder is deleted afterwards whatever
     * happened. Off the EDT, and not to be canceled: a cancel after {@code gh}
     * created the issue would leave an issue with no link stored.
     */
    public @NotNull IssueCreation create(final @NotNull BugRepository repository, final @NotNull String title, final @NotNull String body, final @NotNull List<byte[]> screenshots) {
        final @NotNull Path folder;
        try {
            folder = Files.createTempDirectory("testin-bug-");
        } catch (final IOException ex) {
            return notWritten(ex);
        }

        try {
            Files.writeString(folder.resolve(BODY_FILE), body);
            for (int number = 1; number <= screenshots.size(); number++) {
                Files.write(folder.resolve(BugTemplate.screenshotFile(number)), screenshots.get(number - 1));
            }

            return launcher.run(arguments(repository, title, screenshots.size()), folder)
                    .map(answer -> IssueCreation.of(answer, repository.host(), screenshots.size()))
                    .orElseGet(() -> IssueCreation.failed(Bundle.message("bug.reason.no.gh")));
        } catch (final IOException ex) {
            return notWritten(ex);
        } finally {
            FileUtil.delete(folder.toFile());
        }
    }

    /**
     * The body or a screenshot could not be written for {@code gh}, so nothing
     * was sent.
     */
    private static @NotNull IssueCreation notWritten(final @NotNull IOException ex) {
        Logger.warn("A bug report could not be written for gh: " + ex.getMessage());
        return IssueCreation.failed(Bundle.message("bug.send.not.written", String.valueOf(ex.getMessage())));
    }

    /**
     * The title as one {@code --title=} argument, so a title that starts with a
     * dash is still the title. Each screenshot is attached under the name the
     * body refers to it by.
     */
    static @NotNull List<String> arguments(final @NotNull BugRepository repository, final @NotNull String title, final int screenshots) {
        final @NotNull List<String> arguments = new ArrayList<>(List.of("issue", "create", "--repo", repository.ghRepo(), "--title=" + title, "--body-file", BODY_FILE));
        IntStream.rangeClosed(1, screenshots).forEach(number -> arguments.addAll(List.of("--attach", "./" + BugTemplate.screenshotFile(number))));
        return arguments;
    }

    /**
     * Compared number by number, so {@code 2.100.0} is newer than
     * {@code 2.99.0}. A {@code DEV} build is somebody's own, and counts as new
     * enough.
     */
    static boolean isNewEnough(final @NotNull String versionOutput) {
        final @NotNull String version = versionIn(versionOutput);
        if (version.equals(DEV_BUILD)) return true;

        final @NotNull String[] parts = version.split("[.-]");
        for (int index = 0; index < OLDEST.size(); index++) {
            final int have = index < parts.length && NUMBER.matcher(parts[index]).matches() ? Integer.parseInt(parts[index]) : -1;
            if (have != OLDEST.get(index)) return have > OLDEST.get(index);
        }
        return true;
    }

    private static @NotNull String versionIn(final @NotNull String versionOutput) {
        final @NotNull Matcher version = VERSION.matcher(versionOutput);
        return version.find() ? version.group(1) : "";
    }

    private static @NotNull String oldest() {
        return OLDEST.stream().map(String::valueOf).collect(Collectors.joining("."));
    }

    private static @NotNull Optional<ProcessOutput> start(final @NotNull List<String> arguments, final @NotNull Path workDirectory, final @NotNull ProgressIndicator indicator) {
        final @Nullable File gh = PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS("gh");
        if (gh == null) return Optional.empty();

        final @NotNull GeneralCommandLine command = new GeneralCommandLine(gh.getPath())
                .withParameters(arguments)
                .withWorkingDirectory(workDirectory)
                .withCharset(StandardCharsets.UTF_8)
                .withEnvironment("GH_PROMPT_DISABLED", "1");

        try {
            return Optional.of(new CapturingProcessHandler(command).runProcessWithProgressIndicator(indicator, (int) TIMEOUT.toMillis()));
        } catch (final ExecutionException ex) {
            Logger.warn("gh could not be started: " + ex.getMessage());
            return Optional.empty();
        }
    }
}
