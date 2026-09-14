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

package org.testin.model;

import com.intellij.ide.BrowserUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a GitHub issue's address looks like, and how a tester reads one (#28).
 * <p>
 * One owner, because four places ask: the run grid's Bug Issue column, the
 * details panel's link, the notification that says a bug was reported, and #50's
 * reports - and the output {@code gh} prints is read with the same pattern, so
 * what is stored is exactly what is shown.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BugIssueUrl {

    /**
     * An issue's web address: a host, an owner, a repository, and the number.
     */
    private static final @NotNull Pattern ISSUE =
            Pattern.compile("https?://[A-Za-z0-9.-]+/([A-Za-z0-9._-]+)/([A-Za-z0-9._-]+)/issues/(\\d+)");

    /**
     * {@code owner/repo#123}, which is how GitHub itself writes a reference. An
     * address that is not an issue's reads as itself, so nothing stored is ever
     * hidden behind a reformatting.
     */
    public static @NotNull String reference(final @NotNull String bugIssueUrl) {
        final @NotNull Matcher issue = ISSUE.matcher(bugIssueUrl.strip());

        return issue.matches() ? issue.group(1) + "/" + issue.group(2) + "#" + issue.group(3) : bugIssueUrl;
    }

    /**
     * {@code #123}, how GitHub writes a reference inside one repository. Short
     * enough to sit right after a sentence, which is where the PDF report puts it
     * (#50, D10 revised). An address that is not an issue's reads as itself.
     */
    public static @NotNull String shortReference(final @NotNull String bugIssueUrl) {
        final @NotNull Matcher issue = ISSUE.matcher(bugIssueUrl.strip());

        return issue.matches() ? "#" + issue.group(3) : bugIssueUrl;
    }

    /**
     * The first issue address in some text - what {@code gh issue create}
     * printed - and empty when there is none.
     */
    public static @NotNull Optional<String> firstIn(final @NotNull String text) {
        final @NotNull Matcher issue = ISSUE.matcher(text);

        return issue.find() ? Optional.of(issue.group()) : Optional.empty();
    }

    /**
     * Opens the issue in the browser, through the platform. The details panel's
     * link and the Reported notification's Open both come here.
     */
    public static void open(final @NotNull String bugIssueUrl) {
        BrowserUtil.browse(bugIssueUrl);
    }
}
