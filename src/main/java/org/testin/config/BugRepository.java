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

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The development repository Report Bug files issues in, read out of
 * {@code bugRepoUrl} (#28, P2).
 * <p>
 * Its own parser rather than the {@code RepoUrl} check. That check answers
 * "can Git clone this", which accepts forms {@code gh --repo} cannot use - and
 * once #301 lets it accept {@code file:} URLs and local paths, the gap would
 * widen without anything failing. What {@code gh} needs is exactly a host, an
 * owner and a repository, so an address is kept only when it reduces to those
 * three and nothing more: an address with {@code /issues} after it, a file, a
 * path, or an owner with no repository is refused, and the tester is told.
 *
 * @param host  where the repository lives - {@code github.com}, or a GitHub
 *              Enterprise host
 * @param owner the account or organization that owns it
 * @param name  the repository's own name, without {@code .git}
 */
public record BugRepository(@NotNull String host, @NotNull String owner, @NotNull String name) {

    /**
     * The schemes a web or SSH address arrives with. {@code git@host:owner/repo}
     * has none and is read on its own.
     */
    private static final @NotNull Pattern SCHEME = Pattern.compile("^(https?|ssh)://");

    private static final @NotNull Pattern HOST = Pattern.compile("^[A-Za-z0-9.-]+$");

    /**
     * An owner or a repository name: the characters GitHub allows in either.
     */
    private static final @NotNull Pattern PART = Pattern.compile("^[A-Za-z0-9._-]+$");

    /**
     * The repository an address names, and empty when it does not name exactly
     * one.
     */
    public static @NotNull Optional<BugRepository> of(final @NotNull String address) {
        final @NotNull String value = TestinProjectConfig.withoutCredentials(address.strip());
        if (value.isEmpty()) return Optional.empty();

        if (value.startsWith(TestinProjectConfig.SCP_PREFIX)) {
            final int colon = value.indexOf(':');
            if (colon < 0) return Optional.empty();

            return fromParts(value.substring(TestinProjectConfig.SCP_PREFIX.length(), colon), value.substring(colon + 1));
        }

        final @NotNull Matcher scheme = SCHEME.matcher(value);
        if (!scheme.find()) return Optional.empty();

        final @NotNull String rest = value.substring(scheme.end());
        final int slash = rest.indexOf('/');
        if (slash < 0) return Optional.empty();

        // An account before the host, as ssh://git@host carries, and a port
        // after it, as ssh://host:2222 does, are neither of them the host.
        final @NotNull String authority = rest.substring(0, slash);
        final @NotNull String hostAndPort = authority.substring(authority.lastIndexOf('@') + 1);
        final int port = hostAndPort.indexOf(':');

        return fromParts(port < 0 ? hostAndPort : hostAndPort.substring(0, port), rest.substring(slash + 1));
    }

    private static @NotNull Optional<BugRepository> fromParts(final @NotNull String host, final @NotNull String path) {
        String trimmed = path;
        while (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        while (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (trimmed.endsWith(".git")) trimmed = trimmed.substring(0, trimmed.length() - ".git".length());

        final String @NotNull [] parts = trimmed.split("/", -1);
        if (parts.length != 2 || !HOST.matcher(host).matches()) return Optional.empty();
        if (!PART.matcher(parts[0]).matches() || !PART.matcher(parts[1]).matches()) return Optional.empty();

        return Optional.of(new BugRepository(host, parts[0], parts[1]));
    }

    /**
     * What {@code gh --repo} takes: {@code HOST/OWNER/REPO}, which names the host
     * too, so a GitHub Enterprise repository is filed where it lives.
     */
    public @NotNull String ghRepo() {
        return host + "/" + owner + "/" + name;
    }

    /**
     * How a tester recognizes it: {@code owner/name}.
     */
    public @NotNull String displayName() {
        return owner + "/" + name;
    }
}
