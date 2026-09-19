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

package org.testin.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.util.Bundle;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * What an automation repository's {@code testin.yml} says (#6), as
 * {@link TestinYml} read it. Package-private: nothing outside {@code config} may
 * hold these values, so no caller can decide on its own what a missing one
 * means - it asks {@link TestinYml} (Rule-INTERNAL-089).
 * <p>
 * One test project per automation repository. The repository names it, so the
 * pairing travels with a clone instead of living in one machine's IDE settings,
 * and a tester who opens the repository on a second machine is not asked to pick
 * it again.
 * <p>
 * What is deliberately <b>not</b> here is anything about one machine or one
 * person - the Testin root folder, the tester's name, an account, and above all
 * no secret. This file is committed, so a value in it is
 * shared with everyone who clones and lives in the repository's history forever.
 * <p>
 * <b>{@link #location} is the authority.</b> The file can contradict itself -
 * say {@code remote} and give no address, or {@code local} and leave a URL
 * behind - so the mode decides and the rest is checked against it, with what
 * does not add up written to the log rather than guessed at.
 * <p>
 * Every value is empty rather than null when the file leaves it out, so readers
 * are unconditional.
 *
 * @param location      whether this project is shared at all - through Git, the
 *                      only way there is
 * @param repoUrl       where the test project is cloned from, when it is shared
 * @param testinProject which test project this repository is about, local or
 *                      shared
 * @param bugRepoUrl    the development repository Report Bug files issues in
 *                      (#28), as the tester wrote it with any credentials taken
 *                      out. Kept even when it names no repository, so what is
 *                      wrong with it can be said - {@link #bugRepository()} is
 *                      the question that answers whether it does
 */
record TestinProjectConfig(@NotNull TestinLocation location, @NotNull String repoUrl, @NotNull String testinProject, @NotNull String bugRepoUrl) {

    /**
     * A repository that has said nothing. Every way of failing to read one - no
     * file, no base path, unreadable, malformed - ends here, so no caller has to
     * tell the reasons apart.
     */
    public static final @NotNull TestinProjectConfig EMPTY = new TestinProjectConfig(
            TestinLocation.LOCAL, "", "", "");

    /**
     * A repository whose file is there and could not be read.
     * <p>
     * Says nothing, exactly as {@link #EMPTY} says nothing - a file that will
     * not parse has told us no more than a file that is absent, and every reader
     * of a value gets the same blank either way. What it is not is the same
     * <i>state</i>: a repository nobody has bound yet is ordinary, and one whose
     * file is broken is a mistake somebody can fix.
     * <p>
     * Telling them apart is what stopped a mistyped indent from being answered
     * with "this repository is not bound to a test project" and then quietly
     * bound to whatever single project was under the root - writing a
     * testinProject line into a file that was still broken, on every open, and
     * never saying so (#66, finding 10).
     * <p>
     * <b>Its own instance, asked for by identity.</b> A record compares by
     * value, so this equals EMPTY and must: two configs that say nothing are the
     * same config. Only {@link TestinYml} ever hands this one out, and
     * {@link #isUnreadable()} is the one question that can tell.
     */
    public static final @NotNull TestinProjectConfig UNREADABLE = new TestinProjectConfig(
            TestinLocation.LOCAL, "", "", "");

    /**
     * The forms {@code git clone} is given, and nothing else.
     * <p>
     * The URL arrives in a file that travels with a repository and ends up as an
     * argument to a command, so it is checked here rather than at whichever call
     * site runs it first. Written as the characters a clone URL is made of rather
     * than as the characters to fear: a list of allowed characters cannot be
     * short by one the way a list of forbidden ones can.
     */
    private static final @NotNull Pattern REPO_URL =
            Pattern.compile("^(https://|ssh://|git@)[A-Za-z0-9._~:/?#@%+-]+$");

    public TestinProjectConfig {
        repoUrl = validRepoUrl(repoUrl);

        // Stripped on the way in, as RepoUrl is, and for the same reason: the
        // file is committed. Not refused when it names no repository - the
        // tester is told what is wrong with it where Report Bug would send.
        bugRepoUrl = withoutCredentials(bugRepoUrl);

        report(location, repoUrl);
    }

    /**
     * Says what does not add up, once, where it can be read.
     * <p>
     * Not corrected: a file that says {@code remote} and gives no address has a
     * mistake in it, and quietly behaving as something else would hide the one
     * fact the tester needs.
     */
    private static void report(final @NotNull TestinLocation location, final @NotNull String repoUrl) {
        if (location.isRemote() && repoUrl.isEmpty()) Logger.warn(Bundle.message("config.warn.remote.no.repo.url"));
    }

    /**
     * Absence, as Jackson reports it, becoming the empty value this record
     * promises.
     */
    @JsonCreator
    static @NotNull TestinProjectConfig read(@JsonProperty("location") final @Nullable String location, @JsonProperty("RepoUrl") final @Nullable String repoUrl, @JsonProperty("testinProject") final @Nullable String testinProject, @JsonProperty("bugRepoUrl") final @Nullable String bugRepoUrl) {
        return new TestinProjectConfig(TestinLocation.of(strip(location)),
                strip(repoUrl),
                strip(testinProject),
                strip(bugRepoUrl));
    }

    private static @NotNull String strip(final @Nullable String value) {
        return Objects.requireNonNullElse(value, "").strip();
    }

    private static @NotNull String validRepoUrl(final @NotNull String value) {
        final @NotNull String url = withoutCredentials(value);
        if (url.isEmpty() || REPO_URL.matcher(url).matches()) return url;

        Logger.warn("RepoUrl is not a clone URL and was ignored: " + url);
        return "";
    }

    /**
     * A clone URL with any account and token taken out of it.
     * <p>
     * Git will hand out {@code https://user:token@host/repo} as a remote's URL
     * without being asked, and this file is committed - so a token that reached
     * it would be in the repository's history forever, readable by everyone who
     * clones. Stripped on the way in as well as on the way out, so the record
     * cannot hold one however it arrived: a file somebody committed by hand is
     * as much of a leak as one this plugin wrote.
     * <p>
     * {@code git@github.com} survives. That is the conventional account name for
     * every SSH clone URL and not a secret.
     * <p>
     * Rule-SHARE-004. <b>Over HTTP every account goes, colon or
     * not.</b> The SSH rule was applied to HTTPS too, so only {@code user:secret}
     * was taken out - and {@code https://<token>@github.com/...}, the form
     * GitHub's own documentation clones with, has no colon in it. It passed
     * through into the committed file, and into the history of everyone who
     * cloned it, with no gesture at all: drawing the tree records the remote
     * (#66, finding 164). An HTTPS account has no conventional name that is not
     * a secret, and the credential helper asks for whatever it needs, so taking
     * it out costs nothing.
     */
    static @NotNull String withoutCredentials(final @NotNull String url) {
        final int scheme = url.indexOf("://");
        if (scheme < 0) return url;

        final int start = scheme + "://".length();
        final int end = url.indexOf('/', start);
        final @NotNull String authority = end < 0 ? url.substring(start) : url.substring(start, end);

        final int at = authority.lastIndexOf('@');
        if (at < 0) return url;

        final @NotNull String protocol = url.substring(0, scheme).toLowerCase(Locale.ROOT);
        final boolean overHttp = protocol.equals("https") || protocol.equals("http");
        if (!overHttp && authority.lastIndexOf(':', at) < 0) return url;

        return url.substring(0, start) + authority.substring(at + 1) + (end < 0 ? "" : url.substring(end));
    }

    /**
     * Which test project this repository is about.
     * <p>
     * One key, read the same way whether the project is local or shared. There
     * is one Testin root folder and it holds several projects, so "which one" is
     * a question both have to answer - neither can take it from the root.
     * <p>
     * Empty when the file names none. Which project a repository is about then
     * comes from the tester's choice on this machine ({@code BoundTestProject}),
     * and a clone's name from its own address ({@code CloneTestProject}).
     */
    public @NotNull String projectName() {
        return testinProject;
    }

    /**
     * UC-TREE-PANEL-001.
     * <p>
     * The file is there and could not be read - see {@link #UNREADABLE}.
     * <p>
     * Identity rather than equality, deliberately: by value this config says
     * exactly what an absent one says, which is the point of it.
     */
    @SuppressWarnings("ObjectEquality")
    public boolean isUnreadable() {
        return this == UNREADABLE;
    }

    /**
     * Whether the test project can be fetched when this machine does not have it
     * yet.
     * <p>
     * The mode decides. A project the file calls local is local, whatever
     * address was left in it - so an address commented back in later is the
     * only thing that changes the answer.
     */
    public boolean hasRepoUrl() {
        return location.isRemote() && !repoUrl.isEmpty();
    }

    /**
     * The repository Report Bug files issues in, and empty both when
     * {@code bugRepoUrl} is not set and when it names no repository.
     * {@link #bugRepoUrl()} tells the two apart, which is all a reason needs.
     */
    public @NotNull Optional<BugRepository> bugRepository() {
        return BugRepository.of(bugRepoUrl);
    }
}
