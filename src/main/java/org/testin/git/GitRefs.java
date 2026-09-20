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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import org.testin.config.TestinYml;
import org.testin.util.Bundle;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitRefs {
    private static final @NotNull Pattern HEAD_BRANCH = Pattern.compile("(?m)^\\s*HEAD branch:\\s*(\\S+)\\s*$");

    private static final @NotNull String REMOTES_PREFIX = "remotes/";

    private static final @NotNull String NO_HEAD_BRANCH = "(unknown)";

    private static final @NotNull Set<String> UNMERGED =
            Set.of("DD", "AU", "UD", "UA", "DU", "AA", "UU");

    // UC-SHARE-010, Rule-SHARE-048
    public static @NotNull List<StatusEntry> parseStatus(final @NotNull List<String> porcelainLines) {
        final @NotNull List<StatusEntry> entries = new ArrayList<>();

        for (final String line : porcelainLines) {
            if (line.length() < 4) continue;

            final @NotNull String code = line.substring(0, 2);
            final @NotNull String rawPath = line.substring(3);
            if (code.charAt(0) == '!') continue;

            final int renameArrow = rawPath.indexOf(" -> ");
            final @NotNull String path = unquote(renameArrow < 0 ? rawPath : rawPath.substring(renameArrow + 4));
            if (path.isEmpty()) continue;

            if (renameArrow >= 0 && code.indexOf('R') >= 0) {
                final @NotNull String from = unquote(rawPath.substring(0, renameArrow));
                if (!from.isEmpty()) entries.add(new StatusEntry(DiffType.DELETED, slashed(from)));
            }

            entries.add(new StatusEntry(typeOf(code), slashed(path)));
        }
        return entries;
    }

    public static @NotNull List<String> parseBranches(final @NotNull List<String> branchOutput) {
        return branchOutput.stream()
                .map(line -> line.startsWith("*") ? line.substring(1) : line)
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.contains(" -> "))
                .map(line -> line.startsWith(REMOTES_PREFIX) ? line.substring(REMOTES_PREFIX.length()) : line)
                .distinct()
                .sorted()
                .toList();
    }

    public static boolean hasUnmergedPaths(final @NotNull List<String> porcelainLines) {
        return porcelainLines.stream()
                .filter(line -> line.length() >= 2)
                .map(line -> line.substring(0, 2))
                .anyMatch(UNMERGED::contains);
    }

    private static @NotNull String slashed(final @NotNull String path) {
        return path.replace('\\', '/');
    }

    public static @NotNull List<String> unmergedPaths(final @NotNull List<String> porcelainLines) {
        return porcelainLines.stream()
                .filter(line -> line.length() >= 4)
                .filter(line -> UNMERGED.contains(line.substring(0, 2)))
                .map(line -> unquote(line.substring(3)))
                .filter(path -> !path.isEmpty())
                .map(GitRefs::slashed)
                .toList();
    }

    // UC-SHARE-017
    public static @NotNull String conflictMessage(final @NotNull List<String> unmergedPaths) {
        if (unmergedPaths.isEmpty()) {
            return Bundle.message("git.conflict.unnamed");
        }

        final int shown = Math.min(unmergedPaths.size(), 3);
        final @NotNull String names = String.join(", ", unmergedPaths.subList(0, shown));
        final int rest = unmergedPaths.size() - shown;

        return rest == 0
                ? Bundle.message("git.conflict.named", names)
                : Bundle.message("git.conflict.named.more", names, String.valueOf(rest));
    }

    private static @NotNull DiffType typeOf(final @NotNull String code) {
        if (code.equals("??") || code.indexOf('A') >= 0) return DiffType.ADDED;
        if (code.indexOf('D') >= 0) return DiffType.DELETED;

        if (code.indexOf('R') >= 0) return DiffType.ADDED;

        return DiffType.MODIFIED;
    }

    private static @NotNull String unquote(final @NotNull String rawPath) {
        if (rawPath.length() < 2 || rawPath.charAt(0) != '"' || !rawPath.endsWith("\"")) return rawPath;

        final @NotNull String body = rawPath.substring(1, rawPath.length() - 1);
        final @NotNull ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        for (int i = 0; i < body.length(); i++) {
            final char c = body.charAt(i);
            if (c != '\\' || i + 1 >= body.length()) {
                bytes.writeBytes(String.valueOf(c).getBytes(StandardCharsets.UTF_8));
                continue;
            }

            final char escaped = body.charAt(++i);
            switch (escaped) {
                case 'n' -> bytes.write('\n');
                case 't' -> bytes.write('\t');
                case 'r' -> bytes.write('\r');
                case '"', '\\' -> bytes.write(escaped);
                default -> {
                    if (escaped >= '0' && escaped <= '7' && i + 2 < body.length()) {
                        bytes.write(Integer.parseInt(body.substring(i, i + 3), 8));
                        i += 2;
                    } else {
                        bytes.write(escaped);
                    }
                }
            }
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    public static @NotNull String parseHeadBranch(final @NotNull String remoteShowOutput) {
        final @NotNull Matcher matcher = HEAD_BRANCH.matcher(remoteShowOutput);
        if (!matcher.find()) return "";

        final @NotNull String branch = matcher.group(1);
        return NO_HEAD_BRANCH.equals(branch) ? "" : branch;
    }

    public static @NotNull String chooseRemote(final @NotNull List<String> remotes) {
        final @NotNull List<String> names = remotes.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
        if (names.contains("origin")) return "origin";
        return names.isEmpty() ? "" : names.getFirst();
    }

    // UC-SHARE-008, Rule-SHARE-108
    public static boolean isEmailAddress(final @NotNull String text) {
        final @NotNull String value = text.trim();

        final int at = value.indexOf('@');
        if (at <= 0 || at != value.lastIndexOf('@')) return false;
        if (value.chars().anyMatch(Character::isWhitespace)) return false;

        final @NotNull String domain = value.substring(at + 1);

        return domain.length() >= 3 && domain.indexOf('.') > 0 && !domain.endsWith(".");
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-117
    private static final @NotNull Pattern CLONE_CHARACTERS = Pattern.compile("^[A-Za-z0-9._~:/?#@%+=-]+$");

    // Rule-TREE-PANEL-117
    @SuppressWarnings("HttpUrlsUsage")
    public static boolean isRepositoryUrl(final @NotNull String text) {
        final @NotNull String value = text.trim();
        if (!CLONE_CHARACTERS.matcher(value).matches()) return false;

        return value.startsWith("http://")
                || value.startsWith("https://")
                || value.startsWith("ssh://")
                || value.startsWith("git://")
                || value.startsWith(TestinYml.SCP_PREFIX)
                || value.endsWith(".git");
    }

    public static @NotNull String localNameOf(final @NotNull String remoteBranchName) {
        return remoteBranchName.substring(remoteBranchName.indexOf('/') + 1);
    }

    // UC-TREE-PANEL-026, Rule-TREE-PANEL-086
    public static boolean isRemoteBranch(final @NotNull String branch, final @NotNull List<String> localBranches, final @NotNull List<String> remotes) {
        return !localBranches.contains(branch)
                && remotes.stream().map(String::trim).filter(remote -> !remote.isEmpty()).anyMatch(remote -> branch.startsWith(remote + "/"));
    }

    public static @NotNull Set<String> ancestorDirectories(final @NotNull Collection<String> relativePaths) {
        final @NotNull Set<String> directories = new LinkedHashSet<>();
        directories.add("");

        for (final String path : relativePaths) {
            for (int slash = path.indexOf('/'); slash >= 0; slash = path.indexOf('/', slash + 1)) {
                directories.add(path.substring(0, slash));
            }
        }
        return directories;
    }

    public static @NotNull Set<String> repoRelativePaths(final @NotNull Collection<PendingChange> changes) {
        return changes.stream()
                .map(PendingChange::relativeFilePath)
                .map(path -> path.toString().replace('\\', '/'))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public record StatusEntry(@NotNull DiffType type, @NotNull String path) {
    }
}
