package org.testin.testcase;

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The order a test set is shown in.
 * <p>
 * It used to be a walk: find the case that called itself the head, follow each
 * one to the next, then append whatever the walk never reached. That made the
 * order a relationship between files - so adding a case rewrote its neighbor,
 * two testers adding at once conflicted on a third case, and a single lost
 * pointer left a whole set in an order nobody chose with every row badged
 * Unsorted.
 * <p>
 * Now each case carries its own {@link Rank} and the order is a comparison.
 * Nothing can be unreachable, so nothing can be unsorted: what is in the folder
 * is on the screen, in the order the ranks give.
 */
public final class TestCaseSorter {

    private TestCaseSorter() {
    }

    /**
     * Ranked cases first, in rank order; then any without a rank yet - imported,
     * copied in by hand, brought by a merge - oldest first, so something that
     * has just arrived lands at the end where a tester looks for it. Ties break
     * on the id, so one folder always produces one list, on every machine.
     */
    private static final @NotNull Comparator<TestCaseDto> BY_RANK = Comparator
            .comparing((TestCaseDto tc) -> tc.getOrder().isEmpty())
            .thenComparing(TestCaseDto::getOrder)
            .thenComparing(TestCaseDto::getCreatedAt)
            .thenComparing(TestCaseDto::getId);

    public static @NotNull List<TestCaseDto> sorted(final @NotNull List<TestCaseDto> cases) {
        return cases.stream().sorted(BY_RANK).toList();
    }

    /**
     * Makes the list's own order the ranks, and answers which cases had to
     * change to say so.
     * <p>
     * This is what a drag means: the tester arranged a list, and the cases that
     * were already in the right place keep the rank they had. Only the ones now
     * out of order are given a new rank, between the case before them and the
     * next one that still sorts after it - so dropping one case in a set of two
     * hundred writes one file, and the commit says a case moved rather than
     * that the set was rewritten.
     *
     * @return the cases whose rank changed, in list order - what the caller has
     *         to save, and nothing more
     */
    public static @NotNull List<TestCaseDto> place(final @NotNull List<TestCaseDto> arranged) {
        final List<TestCaseDto> moved = new ArrayList<>();
        String previous = "";

        for (int i = 0; i < arranged.size(); i++) {
            final TestCaseDto testCase = arranged.get(i);

            if (!testCase.getOrder().isEmpty() && testCase.getOrder().compareTo(previous) > 0) {
                previous = testCase.getOrder();
                continue;
            }

            testCase.setOrder(Rank.between(previous, nextRankAfter(arranged, i, previous)));
            previous = testCase.getOrder();
            moved.add(testCase);
        }

        return List.copyOf(moved);
    }

    /**
     * The rank of the first case further down the list that still sorts after
     * {@code previous}, or empty when there is none - the case being placed is
     * going to the end.
     */
    private static @NotNull String nextRankAfter(final @NotNull List<TestCaseDto> arranged, final int from,
                                                 final @NotNull String previous) {
        for (int i = from + 1; i < arranged.size(); i++) {
            final String rank = arranged.get(i).getOrder();
            if (!rank.isEmpty() && rank.compareTo(previous) > 0) return rank;
        }

        return "";
    }

    /**
     * Ranks a list from nothing, evenly spread - an import, or a set being
     * placed for the first time.
     */
    public static void rankAll(final @NotNull List<TestCaseDto> ordered) {
        final List<String> ranks = Rank.spread(ordered.size());

        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setOrder(ranks.get(i));
        }
    }
}
