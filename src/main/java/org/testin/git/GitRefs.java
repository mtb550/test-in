package org.testin.git;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Pure Git naming and selection rules, extracted from the Git4Idea-backed
 * services so they are unit-testable without an IDE or a repository.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitRefs {

    private static final Pattern HEAD_BRANCH = Pattern.compile("(?m)^\\s*HEAD branch:\\s*(\\S+)\\s*$");

    /**
     * The HEAD branch reported by {@code git remote show}, or {@code null}.
     */
    public static @Nullable String parseHeadBranch(final @NotNull String remoteShowOutput) {
        final Matcher matcher = HEAD_BRANCH.matcher(remoteShowOutput);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * The remote to sync with: {@code origin} when present, otherwise the
     * first remote, otherwise {@code null}.
     */
    public static @Nullable String chooseRemote(final @NotNull List<String> remotes) {
        final List<String> names = remotes.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
        if (names.contains("origin")) return "origin";
        return names.isEmpty() ? null : names.getFirst();
    }

    /**
     * True when the text names a repository to clone rather than a project to
     * create.
     * <p>
     * The create-project dialog takes one field for both, so this is what
     * decides which happens. Deliberately narrow: a project name is free text
     * typed by the tester, and mistaking one for a URL would send them to a
     * clone they never asked for.
     */
    // "http://" here is a scheme being recognised, not a link being followed:
    // this decides whether the tester typed a clone URL. Refusing to match it
    // would not make anything more secure, it would stop plain-http remotes
    // being clonable at all.
    @SuppressWarnings("HttpUrlsUsage")
    public static boolean isRepositoryUrl(final @NotNull String text) {
        final String value = text.trim();
        return value.startsWith("http://")
                || value.startsWith("https://")
                || value.startsWith("ssh://")
                || value.startsWith("git://")
                || value.startsWith("git@")
                || value.endsWith(".git");
    }

    public static @NotNull String localNameOf(final @NotNull String remoteBranchName) {
        return remoteBranchName.substring(remoteBranchName.indexOf('/') + 1);
    }

    /**
     * The forward-slashed repository-relative paths behind the selected
     * changes, deduplicated in selection order.
     */
    public static @NotNull Set<String> repoRelativePaths(final @NotNull Collection<TestCaseDiff> changes) {
        return changes.stream()
                .map(TestCaseDiff::relativeFilePath)
                .map(path -> path.toString().replace('\\', '/'))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
