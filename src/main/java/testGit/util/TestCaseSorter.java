package testGit.util;

import com.google.common.collect.Maps;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.notifications.Notifier;

import java.util.*;

public class TestCaseSorter {
    public static SortResult sortTestCases(final List<TestCaseDto> unsortedList) {
        if (unsortedList == null || unsortedList.isEmpty()) {
            return new SortResult(new ArrayList<>(), new HashSet<>());
        }

        Map<UUID, TestCaseDto> idMap = Maps.newHashMapWithExpectedSize(unsortedList.size());
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
            Notifier.warn("Warning", "No Head found in test cases.");
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

    public record SortResult(List<TestCaseDto> sortedList, Set<UUID> unsortedIds) {
    }
}