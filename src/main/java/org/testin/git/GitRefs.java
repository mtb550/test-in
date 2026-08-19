package org.testin.git;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
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

    private static final @NotNull String REMOTES_PREFIX = "remotes/";

    /**
     * What {@code git remote show} prints for a remote that has no branches.
     */
    private static final @NotNull String NO_HEAD_BRANCH = "(unknown)";

    /**
     * The porcelain codes for a path with a conflict: either side added, either
     * side deleted, or both changed it.
     */
    private static final @NotNull Set<String> UNMERGED =
            Set.of("DD", "AU", "UD", "UA", "DU", "AA", "UU");

    /**
     * Reads {@code git status --porcelain -uall} into what changed.
     * <p>
     * Pure text, so the rules can be asserted without a repository. The three
     * that matter, and that cost real debugging when they are wrong:
     * <ul>
     *   <li>{@code ??} is untracked, which is what a brand-new test case is
     *       until something stages it - so it is an addition, not nothing.</li>
     *   <li>A path containing a space comes back wrapped in double quotes with
     *       C-style escapes, and every test set named with a space produces one.</li>
     *   <li>A rename is reported as {@code old -> new}, and the new path is the
     *       one the review is about.</li>
     * </ul>
     */
    public static @NotNull List<StatusEntry> parseStatus(final @NotNull List<String> porcelainLines) {
        final List<StatusEntry> entries = new ArrayList<>();

        for (final String line : porcelainLines) {
            if (line.length() < 4) continue;

            final String code = line.substring(0, 2);
            final String rawPath = line.substring(3);
            if (code.charAt(0) == '!') continue;

            final int renameArrow = rawPath.indexOf(" -> ");
            final String path = unquote(renameArrow < 0 ? rawPath : rawPath.substring(renameArrow + 4));
            if (path.isEmpty()) continue;

            entries.add(new StatusEntry(typeOf(code), path.replace('\\', '/')));
        }
        return entries;
    }

    /**
     * The branch names from {@code git branch -a}, as the tester picks them:
     * {@code main} for a local branch and {@code origin/main} for a remote one.
     * <p>
     * Two things are dropped. The {@code * } marking the current branch, which is
     * decoration. And the symbolic ref line {@code remotes/origin/HEAD -> origin/main},
     * which names no branch of its own - checking it out detaches HEAD, and it
     * would sit in the list looking like a third branch.
     */
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

    /**
     * True when {@code git status --porcelain} reports a path both sides
     * touched - the state a pull leaves behind when it cannot merge, and the
     * only thing that makes the abort-or-continue offer worth showing.
     */
    public static boolean hasUnmergedPaths(final @NotNull List<String> porcelainLines) {
        return porcelainLines.stream()
                .filter(line -> line.length() >= 2)
                .map(line -> line.substring(0, 2))
                .anyMatch(UNMERGED::contains);
    }

    private static @NotNull DiffType typeOf(final @NotNull String code) {
        if (code.equals("??") || code.indexOf('A') >= 0) return DiffType.ADDED;
        if (code.indexOf('D') >= 0) return DiffType.DELETED;
        return DiffType.MODIFIED;
    }

    /**
     * Undoes git's path quoting: the whole path in double quotes, with
     * backslash escapes and non-ASCII bytes as octal. The octal matters because
     * a test set named in Arabic arrives entirely as escapes, and decoding the
     * bytes as UTF-8 is the only way back to the name on disk.
     */
    private static @NotNull String unquote(final @NotNull String rawPath) {
        if (rawPath.length() < 2 || rawPath.charAt(0) != '"' || !rawPath.endsWith("\"")) return rawPath;

        final String body = rawPath.substring(1, rawPath.length() - 1);
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

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

    /**
     * The HEAD branch reported by {@code git remote show}, or {@code null}.
     * <p>
     * A remote with no commits reports {@code HEAD branch: (unknown)} - it has no
     * branches yet, so there is nothing to name. Read literally that is a branch
     * called {@code (unknown)}, which is what a first push tried to pull from:
     * "couldn't find remote ref (unknown)". Null instead, so the caller falls
     * back to the branch checked out here, which is the one being pushed.
     */
    public static @Nullable String parseHeadBranch(final @NotNull String remoteShowOutput) {
        final Matcher matcher = HEAD_BRANCH.matcher(remoteShowOutput);
        if (!matcher.find()) return null;

        final String branch = matcher.group(1);
        return NO_HEAD_BRANCH.equals(branch) ? null : branch;
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
    // "http://" here is a scheme being recognized, not a link being followed:
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
     * Every directory the given repository-relative files sit under, itself
     * repository-relative, with the repository root as the empty string.
     * <p>
     * Used to find the marker files that have to travel with a commit: a test
     * case is a file in a directory, and that directory is only a test set
     * because a {@code .ts} sits beside it.
     */
    public static @NotNull Set<String> ancestorDirectories(final @NotNull Collection<String> relativePaths) {
        final Set<String> directories = new LinkedHashSet<>();
        directories.add("");

        for (final String path : relativePaths) {
            for (int slash = path.indexOf('/'); slash >= 0; slash = path.indexOf('/', slash + 1)) {
                directories.add(path.substring(0, slash));
            }
        }
        return directories;
    }

    /**
     * The forward-slashed repository-relative paths behind the selected
     * changes, deduplicated in selection order.
     */

    public static @NotNull Set<String> repoRelativePaths(final @NotNull Collection<PendingChange> changes) {
        return changes.stream()
                .map(PendingChange::relativeFilePath)
                .map(path -> path.toString().replace('\\', '/'))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * One line of {@code git status --porcelain}: what happened to a file, and
     * which file, with the path already unquoted and slashed for comparison.
     */
    public record StatusEntry(@NotNull DiffType type, @NotNull String path) {
    }
}
