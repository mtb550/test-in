package org.testin.testcase;

import com.google.common.collect.Maps;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.util.*;

public class TestCaseSorter {

    /**
     * Writes the order back onto the cases: the first is the head, each one
     * points at the next, and the last points at nothing.
     * <p>
     * The counterpart of {@link #sortTestCases}, which reads the chain. Sorting
     * a set and relinking it is how a broken chain is repaired - the sort puts
     * everything nothing points at on the end, and this makes that the order.
     * <p>
     * Here rather than inside the indexer's writer, because a merge repairs a
     * chain too: two testers who each add a case to the same test set both
     * rewrite the tail's pointer, and whichever the merge keeps leaves the other
     * case pointed at by nothing (#90).
     */
    public static void relink(final @NotNull List<TestCaseDto> sortedList) {
        for (int i = 0; i < sortedList.size(); i++) {
            sortedList.get(i).setIsHead(i == 0);
            sortedList.get(i).setNext(i < sortedList.size() - 1 ? sortedList.get(i + 1).getId() : null);
        }
    }
    /**
     * The order, and a word to the tester when there is none to read.
     * <p>
     * The message belongs to the surfaces a tester is looking at - the editor
     * opening a set - and not to sorting itself, which is why the rule below
     * takes no project. A conflict repair sorts too, and a balloon in the middle
     * of a merge would be about something the tester did not do.
     */
    public static @NotNull SortResult sortTestCases(final @NotNull Project p, final @NotNull List<TestCaseDto> unsortedList) {
        final SortResult result = sortTestCases(unsortedList);

        if (!unsortedList.isEmpty() && result.unsortedIds().size() == unsortedList.size()) {
            Services.getInstance(p, Notifier.class).softShow(p, "Order Unknown",
                    "These test cases have no starting point, so they are shown in file order.");
        }

        return result;
    }

    /**
     * The linked-list order: the chain from the head, then everything nothing
     * points at, which is what a case added while the chain was being rewritten
     * looks like.
     */
    public static @NotNull SortResult sortTestCases(final @NotNull List<TestCaseDto> unsortedList) {
        if (unsortedList.isEmpty()) {
            return new SortResult(new ArrayList<>(), new HashSet<>());
        }

        final Map<UUID, TestCaseDto> idMap = Maps.newHashMapWithExpectedSize(unsortedList.size());
        TestCaseDto head = null;

        for (final TestCaseDto tc : unsortedList) {
            idMap.put(tc.getId(), tc);
            if (Boolean.TRUE.equals(tc.getIsHead())) {
                head = tc;
            }
        }

        final List<TestCaseDto> sortedList = new ArrayList<>(unsortedList.size());
        final Set<UUID> visited = new HashSet<>();
        final Set<UUID> unsortedIds = new HashSet<>();

        if (head == null) {
            unsortedList.forEach(tc -> unsortedIds.add(tc.getId()));
            return new SortResult(unsortedList, unsortedIds);
        }

        TestCaseDto current = head;
        while (current != null && !visited.contains(current.getId())) {
            sortedList.add(current);
            visited.add(current.getId());

            final UUID nextUuid = current.getNext();
            current = (nextUuid != null) ? idMap.get(nextUuid) : null;
        }

        if (sortedList.size() < unsortedList.size()) {
            for (final TestCaseDto tc : unsortedList) {
                if (!visited.contains(tc.getId())) {
                    sortedList.add(tc);
                    unsortedIds.add(tc.getId());
                }
            }
        }

        return new SortResult(sortedList, unsortedIds);
    }
}