package testGit.util;

import testGit.pojo.TestCase;

import java.util.*;

public class TestCaseSorter {

    public static List<TestCase> sortTestCases(List<TestCase> unsortedList) {
        if (unsortedList == null || unsortedList.isEmpty()) {
            System.out.println("no test cases");
            return new ArrayList<>();
        }

        Map<String, TestCase> idMap = new HashMap<>();
        TestCase head = null;

        for (TestCase tc : unsortedList) {
            idMap.put(tc.getId(), tc);
            if (tc.getIsHead() != null && tc.getIsHead()) {
                head = tc;
            }
        }

        if (head == null) {
            Notifier.warn("Warning: ", "No Head found in test cases.");
            return unsortedList;
        }

        List<TestCase> sortedList = new ArrayList<>();
        TestCase current = head;

        Set<String> visited = new HashSet<>();

        while (current != null && !visited.contains(current.getId())) {
            sortedList.add(current);
            visited.add(current.getId());

            UUID nextUuid = current.getNext();
            if (nextUuid != null) {
                current = idMap.get(nextUuid.toString());
            } else {
                current = null;
            }
        }

        if (sortedList.size() < unsortedList.size()) {
            for (TestCase tc : unsortedList) {
                if (!visited.contains(tc.getId())) {
                    sortedList.add(tc);
                }
            }
        }

        return sortedList;
    }
}
