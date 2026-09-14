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

import com.intellij.execution.process.ProcessOutput;
import org.jetbrains.annotations.NotNull;
import org.testin.model.BugIssueUrl;
import org.testin.util.Bundle;

import java.util.Optional;
import java.util.stream.IntStream;

/**
 * What came of asking {@code gh} to file an issue (#28).
 *
 * @param url         the new issue, and empty when none is known to exist
 * @param notUploaded how many screenshots {@code gh} named as not uploaded
 * @param problem     what went wrong, in {@code gh}'s words or Testin's, and
 *                    empty when nothing did
 */
public record IssueCreation(@NotNull Optional<String> url, int notUploaded, @NotNull String problem) {

    /**
     * What {@code gh} exits with when it needs a sign-in.
     */
    private static final int SIGNED_OUT = 4;

    static @NotNull IssueCreation failed(final @NotNull String problem) {
        return new IssueCreation(Optional.empty(), 0, problem);
    }

    /**
     * Read the way {@code gh}'s help describes it: an issue address on standard
     * output means the issue exists, even when {@code gh} exits non-zero because
     * some attachments did not upload. A timeout means nobody knows.
     */
    static @NotNull IssueCreation of(final @NotNull ProcessOutput answer, final @NotNull String host, final int screenshots) {
        if (answer.isTimeout()) return failed(Bundle.message("bug.send.timed.out", GitHubCli.TIMEOUT.toSeconds()));

        final @NotNull String said = answer.getStderr().strip();
        final @NotNull Optional<String> url = BugIssueUrl.firstIn(answer.getStdout());
        if (url.isPresent()) {
            return answer.getExitCode() == 0 ? new IssueCreation(url, 0, "") : new IssueCreation(url, notUploaded(said, screenshots), said);
        }

        if (answer.getExitCode() == SIGNED_OUT) return failed(Bundle.message("bug.reason.signed.out", host));

        return failed(said.isEmpty() ? Bundle.message("bug.send.failed", answer.getExitCode()) : said);
    }

    /**
     * Counted from the screenshot names {@code gh} mentions. Its help promises
     * the address and a non-zero exit, not the shape of the error, so a failure
     * that names no file counts none.
     */
    private static int notUploaded(final @NotNull String said, final int screenshots) {
        return (int) IntStream.rangeClosed(1, screenshots)
                .filter(number -> said.contains(BugTemplate.screenshotFile(number)))
                .count();
    }
}
