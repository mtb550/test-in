package org.testin.git;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.testcase.TestCaseSorter;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.function.Consumer;

/**
 * Turns a stopped pull into a test case question, or into no question at all
 * (#90).
 * <p>
 * Git holds three versions of every conflicted file - the common ancestor, this
 * machine's, and the one the pull brought - and {@link TestCaseMerge} settles
 * almost every field between them. What this adds is the round trip: reading the
 * three stages, writing the merged case back, staging it, and asking the tester
 * only about the files that still hold a disagreement.
 * <p>
 * File access is direct, which the architecture rule allows this package: the
 * merged text is exactly what Git must see staged, and going through the indexer
 * would rewrite it in the plugin's own formatting. The cache is rebuilt by the
 * re-index that follows the rebase.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConflictResolution {

    /**
     * Git's three sides of a conflicted file, in the index: the common ancestor,
     * ours, theirs. During a rebase "ours" is the branch being replayed onto and
     * "theirs" is the tester's own commits, which is why nothing downstream is
     * labelled by stage number.
     */
    private static final int BASE = 1;
    private static final int REMOTE = 2;
    private static final int MINE = 3;

    /**
     * One conflicted test case the merge could not finish on its own.
     */
    private record Pending(@NotNull String relativePath, @NotNull String name, @NotNull ObjectNode merged,
                           @NotNull List<TestCaseMerge.Question> questions, @NotNull String theirs) {
    }

    /**
     * Resolves what it can and asks about the rest.
     * <p>
     * Called on a background thread - it reads Git and writes files - and hands
     * back on the EDT. The two outcomes are separate because they are different
     * situations for the caller: everything resolved means the rebase can go on,
     * and anything left means it must not.
     *
     * @param conflicting the paths Git reports as conflicting
     * @param onResolved  run when nothing conflicting is left
     * @param onLeftOver  given whatever could not be resolved here - a run, a
     *                    marker, a file that is not Testin's at all
     */
    public static void resolve(final @NotNull Project p, final @NotNull Path repositoryPath,
                               final @NotNull List<String> conflicting, final @NotNull Runnable onResolved,
                               final @NotNull Consumer<List<String>> onLeftOver) {
        final GitRepositoryService git = new GitRepositoryService(p);
        final Mapper mapper = Services.getInstance(p, Mapper.class);

        final List<String> leftOver = new ArrayList<>();
        final List<Pending> pending = new ArrayList<>();

        // Which test sets a conflict landed in. The chain is a property of the
        // set, so the repair happens once per set and not once per file.
        final Set<String> resolvedSets = new LinkedHashSet<>();

        for (final String relativePath : conflicting) {
            if (!isTestCase(relativePath)) {
                leftOver.add(relativePath);
                continue;
            }

            final String base = git.stageContent(repositoryPath, relativePath, BASE);
            final String mine = git.stageContent(repositoryPath, relativePath, MINE);
            final String theirs = git.stageContent(repositoryPath, relativePath, REMOTE);

            if (mine.isBlank() || theirs.isBlank()) {
                // One side deleted the case and the other edited it. Which of
                // those a team meant is not a field question, so it stays with
                // the tester.
                leftOver.add(relativePath);
                continue;
            }

            final TestCaseMerge.Merge merge = TestCaseMerge.of(mapper, base, mine, theirs);

            resolvedSets.add(testSetOf(relativePath));

            if (!merge.isSettled()) {
                pending.add(new Pending(relativePath, name(mapper, mine, relativePath), merge.merged(),
                        merge.questions(), theirs));
                continue;
            }

            if (!keep(p, git, repositoryPath, relativePath, merge.merged())) leftOver.add(relativePath);
        }

        ApplicationManager.getApplication().invokeLater(() ->
                ask(p, git, mapper, repositoryPath, pending, leftOver, resolvedSets, onResolved, onLeftOver));
    }

    /**
     * Asks about one conflicted case, then the next. One dialog at a time: three
     * dialogs at once would be three questions with no order to them, and each
     * answer is written and staged before the following question opens.
     */
    private static void ask(final @NotNull Project p, final @NotNull GitRepositoryService git,
                            final @NotNull Mapper mapper, final @NotNull Path repositoryPath,
                            final @NotNull List<Pending> pending, final @NotNull List<String> leftOver,
                            final @NotNull Set<String> resolvedSets, final @NotNull Runnable onResolved,
                            final @NotNull Consumer<List<String>> onLeftOver) {
        if (pending.isEmpty()) {
            // Every conflict in these sets is settled now, so the chain can be
            // read as a whole - which is the only level it means anything at.
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                repairChains(git, mapper, repositoryPath, resolvedSets);

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (leftOver.isEmpty()) onResolved.run();
                    else onLeftOver.accept(List.copyOf(leftOver));
                });
            });
            return;
        }

        final Pending next = pending.getFirst();
        final List<Pending> rest = pending.subList(1, pending.size());

        new ResolveConflictDialog(p, next.name(), next.questions(), takeTheirs -> {
            for (final TestCaseMerge.Question question : next.questions()) {
                TestCaseMerge.answer(mapper, next.merged(), question, takeTheirs.contains(question.field()),
                        next.theirs());
            }

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                final boolean written = keep(p, git, repositoryPath, next.relativePath(), next.merged());
                final List<String> stillLeft = new ArrayList<>(leftOver);
                if (!written) stillLeft.add(next.relativePath());

                ApplicationManager.getApplication().invokeLater(() ->
                        ask(p, git, mapper, repositoryPath, new ArrayList<>(rest), stillLeft, resolvedSets,
                                onResolved, onLeftOver));
            });
        }).show();
    }

    /**
     * Puts the order of every test set a conflict landed in back together.
     * <p>
     * A merge decides one file at a time and the order is not in one file: it is
     * a chain across the set, held in {@code isHead} and {@code next}. Two
     * testers who each add a case to the same set both rewrite the tail's
     * pointer, so whichever the merge keeps leaves the other case pointed at by
     * nothing - present, committed, and nowhere in the order.
     * <p>
     * So the set is read as a whole once its conflicts are settled, sorted the
     * way the editor sorts it - the chain first, then everything nothing points
     * at - and relinked along that order. Both testers' cases end up in the
     * list, the one that lost the pointer last.
     * <p>
     * Only the files that actually changed are written and staged, so the repair
     * is part of the commit that caused it rather than a modification left
     * behind after the rebase.
     */
    private static void repairChains(final @NotNull GitRepositoryService git, final @NotNull Mapper mapper,
                                     final @NotNull Path repositoryPath, final @NotNull Set<String> testSets) {
        for (final String testSet : testSets) {
            if (testSet.isEmpty()) continue;

            final Path directory = repositoryPath.resolve(testSet);
            final Map<Path, TestCaseDto> cases = read(mapper, directory);
            if (cases.size() < 2) continue;

            final List<TestCaseDto> sorted = TestCaseSorter.sortTestCases(new ArrayList<>(cases.values())).sortedList();
            TestCaseSorter.relink(sorted);

            cases.forEach((file, testCase) -> rewriteIfChanged(git, mapper, repositoryPath, file, testCase));
        }
    }

    /**
     * Every test case in a test set directory, as it stands on disk after the
     * merge. Read here rather than from the indexer's cache: the files were just
     * rewritten underneath it, and the cache is rebuilt after the rebase.
     */
    private static @NotNull Map<Path, TestCaseDto> read(final @NotNull Mapper mapper, final @NotNull Path directory) {
        final Map<Path, TestCaseDto> cases = new LinkedHashMap<>();

        try (Stream<Path> files = Files.list(directory)) {
            files.filter(file -> file.getFileName().toString().endsWith(".json")).sorted().forEach(file -> {
                try {
                    cases.put(file, mapper.readValue(Files.readString(file, StandardCharsets.UTF_8), TestCaseDto.class));
                } catch (final Exception ex) {
                    Logger.warn("Skipping " + file.getFileName() + " while repairing the order: " + ex.getMessage());
                }
            });

        } catch (final IOException ex) {
            Logger.error("Could not read " + directory + " to repair the order: " + ex.getMessage());
        }

        return cases;
    }

    /**
     * Writes a case whose place in the chain moved, and stages it. A case the
     * repair did not move is left exactly as it is - rewriting it would put an
     * untouched file in the commit and the diff of a rebase would stop being
     * readable.
     */
    private static void rewriteIfChanged(final @NotNull GitRepositoryService git, final @NotNull Mapper mapper,
                                         final @NotNull Path repositoryPath, final @NotNull Path file,
                                         final @NotNull TestCaseDto testCase) {
        try {
            final String relinked = mapper.writeValueAsString(testCase);
            if (relinked.equals(Files.readString(file, StandardCharsets.UTF_8))) return;

            Files.writeString(file, relinked, StandardCharsets.UTF_8);
            git.stageResolved(repositoryPath, repositoryPath.relativize(file).toString().replace('\\', '/'));

            Logger.info("Repaired the order of " + file.getFileName());

        } catch (final IOException ex) {
            Logger.error("Could not repair the order in " + file + ": " + ex.getMessage());
        }
    }

    /**
     * The test set a conflicted file sits in, as a repository-relative path.
     */
    private static @NotNull String testSetOf(final @NotNull String relativePath) {
        final int lastSlash = relativePath.lastIndexOf('/');
        return lastSlash < 0 ? "" : relativePath.substring(0, lastSlash);
    }

    /**
     * Writes the merged case and stages it, which is what tells Git the conflict
     * is over. Answers whether both halves worked - a file written and not
     * staged would stop the rebase again with no conflict left to see.
     */
    private static boolean keep(final @NotNull Project p, final @NotNull GitRepositoryService git,
                                final @NotNull Path repositoryPath, final @NotNull String relativePath,
                                final @NotNull ObjectNode merged) {
        try {
            Files.writeString(repositoryPath.resolve(relativePath), merged.toPrettyString(), StandardCharsets.UTF_8);

        } catch (final IOException ex) {
            Logger.error("Could not write the merged test case " + relativePath + ": " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, "Merge Failed",
                    "Could not write " + relativePath + ": " + ex.getMessage());
            return false;
        }

        if (git.stageResolved(repositoryPath, relativePath)) return true;

        Logger.error("Merged but could not stage " + relativePath);
        return false;
    }

    /**
     * What to call the case in a dialog title: its description, or the file name
     * when the side being read will not parse.
     */
    private static @NotNull String name(final @NotNull Mapper mapper, final @NotNull String json,
                                        final @NotNull String relativePath) {
        final String description = mapper.readTree(json).path("description").asText("");
        if (!description.isBlank()) return description;

        final Path path = Path.of(relativePath);
        return path.getFileName().toString();
    }

    /**
     * Whether the conflict is in a test case at all. A run's results and a
     * marker are different shapes with different rules, and a file that is not
     * Testin's is nobody's business here.
     */
    private static boolean isTestCase(final @NotNull String relativePath) {
        return relativePath.endsWith(".json") && relativePath.contains("Test Cases/");
    }
}
