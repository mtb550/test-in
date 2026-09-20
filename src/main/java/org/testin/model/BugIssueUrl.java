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
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BugIssueUrl {
    private static final @NotNull Pattern ISSUE =
            Pattern.compile("https?://[A-Za-z0-9.-]+/([A-Za-z0-9._-]+)/([A-Za-z0-9._-]+)/issues/(\\d+)");

    public static @NotNull String reference(final @NotNull String bugIssueUrl) {
        return issue(bugIssueUrl).map(issue -> issue.group(1) + "/" + issue.group(2) + "#" + issue.group(3)).orElse(bugIssueUrl);
    }

    public static @NotNull String shortReference(final @NotNull String bugIssueUrl) {
        return issue(bugIssueUrl).map(issue -> "#" + issue.group(3)).orElse(bugIssueUrl);
    }

    private static @NotNull Optional<MatchResult> issue(final @NotNull String bugIssueUrl) {
        final @NotNull Matcher issue = ISSUE.matcher(bugIssueUrl.strip());
        return issue.matches() ? Optional.of(issue.toMatchResult()) : Optional.empty();
    }

    public static @NotNull Optional<String> firstIn(final @NotNull String text) {
        final @NotNull Matcher issue = ISSUE.matcher(text);

        return issue.find() ? Optional.of(issue.group()) : Optional.empty();
    }

    public static void open(final @NotNull String bugIssueUrl) {
        BrowserUtil.browse(bugIssueUrl);
    }
}
