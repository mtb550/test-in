package org.testin.git;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
     * labeled by stage number.
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
     *                    marker, or a file the plugin never wrote
     */
    public static void resolve(final @NotNull Project p, final @NotNull Path repositoryPath,
                               final @NotNull List<String> conflicting, final @NotNull Runnable onResolved,
                               final @NotNull Consumer<List<String>> onLeftOver) {
        final @NotNull GitRepositoryService git = new GitRepositoryService(p);
        final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);

        final @NotNull List<String> leftOver = new ArrayList<>();
        final @NotNull List<Pending> pending = new ArrayList<>();

        for (final String relativePath : conflicting) {
            if (!isTestCase(relativePath)) {
                leftOver.add(relativePath);
                continue;
            }

            final @NotNull String base = git.stageContent(repositoryPath, relativePath, BASE);
            final @NotNull String mine = git.stageContent(repositoryPath, relativePath, MINE);
            final @NotNull String theirs = git.stageContent(repositoryPath, relativePath, REMOTE);

            if (mine.isBlank() || theirs.isBlank()) {
                // One side deleted the case and the other edited it. Which of
                // those a team meant is not a field question, so it stays with
                // the tester.
                leftOver.add(relativePath);
                continue;
            }

            final @NotNull TestCaseMerge.Merge merge = TestCaseMerge.of(mapper, base, mine, theirs);

            if (!merge.isSettled()) {
                pending.add(new Pending(relativePath, name(mapper, mine, relativePath), merge.merged(),
                        merge.questions(), theirs));
                continue;
            }

            if (!keep(p, git, repositoryPath, relativePath, merge.merged())) leftOver.add(relativePath);
        }

        ApplicationManager.getApplication().invokeLater(() ->
                ask(p, git, mapper, repositoryPath, pending, leftOver, onResolved, onLeftOver));
    }

    /**
     * Asks about one conflicted case, then the next. One dialog at a time: three
     * dialogs at once would be three questions with no order to them, and each
     * answer is written and staged before the following question opens.
     */
    private static void ask(final @NotNull Project p, final @NotNull GitRepositoryService git,
                            final @NotNull Mapper mapper, final @NotNull Path repositoryPath,
                            final @NotNull List<Pending> pending, final @NotNull List<String> leftOver,
                            final @NotNull Runnable onResolved, final @NotNull Consumer<List<String>> onLeftOver) {
        if (pending.isEmpty()) {
            // Nothing to put back together. A case carries its own position, so
            // two testers who each added one to the same set wrote two files
            // with two ranks and never touched a third - the order comes out of
            // the merge intact, with no set-wide repair to run.
            if (leftOver.isEmpty()) onResolved.run();
            else onLeftOver.accept(List.copyOf(leftOver));
            return;
        }

        final @NotNull Pending next = pending.getFirst();
        final @NotNull List<Pending> rest = pending.subList(1, pending.size());

        new ResolveConflictDialog(p, next.name(), next.questions(), takeTheirs -> {
            for (final TestCaseMerge.Question question : next.questions()) {
                TestCaseMerge.answer(mapper, next.merged(), question, takeTheirs.contains(question.field()),
                        next.theirs());
            }

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                final boolean written = keep(p, git, repositoryPath, next.relativePath(), next.merged());
                final @NotNull List<String> stillLeft = new ArrayList<>(leftOver);
                if (!written) stillLeft.add(next.relativePath());

                ApplicationManager.getApplication().invokeLater(() ->
                        ask(p, git, mapper, repositoryPath, new ArrayList<>(rest), stillLeft, onResolved, onLeftOver));
            });
        }).show();
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
        final @NotNull String description = mapper.readTree(json).path("description").asText("");
        if (!description.isBlank()) return description;

        final @NotNull Path path = Path.of(relativePath);
        return path.getFileName().toString();
    }

    /**
     * Whether the conflict is in a test case at all. A run's results and a
     * marker are different shapes with different rules, and a file from outside
     * the plugin is somebody else's business.
     */
    private static boolean isTestCase(final @NotNull String relativePath) {
        return relativePath.endsWith(".json") && relativePath.contains("Test Cases/");
    }
}
