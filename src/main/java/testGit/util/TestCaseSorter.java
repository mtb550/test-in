package testGit.util;

import testGit.pojo.TestCase;

import java.util.*;

public class TestCaseSorter {

    public static List<TestCase> sortTestCases(List<TestCase> unsortedList) {
        if (unsortedList == null || unsortedList.isEmpty()) {
            System.out.println("no test cases");
            return new ArrayList<>();
        }

        // 1. Create a Map for fast lookup by ID (String -> TestCase)
        Map<String, TestCase> idMap = new HashMap<>();
        TestCase head = null;

        for (TestCase tc : unsortedList) {
            idMap.put(tc.getId(), tc);
            // Identify the starting point
            if (tc.getIsHead() != null && tc.getIsHead()) {
                head = tc;
            }
        }

        // Fallback: If no head is found, the chain is broken.
        // Use the first item or return unsorted to avoid data loss.
        if (head == null) {
            System.err.println("Warning: No Head found in test cases.");
            return unsortedList;
        }

        // 2. Reconstruct the list by following the 'next' pointers
        List<TestCase> sortedList = new ArrayList<>();
        TestCase current = head;

        // Use a Set to prevent infinite loops if there's a circular reference
        Set<String> visited = new HashSet<>();

        while (current != null && !visited.contains(current.getId())) {
            sortedList.add(current);
            visited.add(current.getId());

            UUID nextUuid = current.getNext();
            if (nextUuid != null) {
                current = idMap.get(nextUuid.toString());
            } else {
                current = null; // End of the chain
            }
        }

        if (sortedList.size() < unsortedList.size()) {
            for (TestCase tc : unsortedList) {
                if (!visited.contains(tc.getId())) {
                    // These are 'orphaned' test cases that aren't in the chain
                    sortedList.add(tc);
                }
            }
        }

        return sortedList;
    }
}
