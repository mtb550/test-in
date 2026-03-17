package testGit.util;

import testGit.pojo.mappers.TestCaseJsonMapper;

import java.util.*;

public class TestCaseSorter {

    public static List<TestCaseJsonMapper> sortTestCases(List<TestCaseJsonMapper> unsortedList) {
        if (unsortedList == null || unsortedList.isEmpty()) {
            System.out.println("no test cases");
            return new ArrayList<>();
        }

        Map<String, TestCaseJsonMapper> idMap = new HashMap<>();
        TestCaseJsonMapper head = null;

        for (TestCaseJsonMapper tc : unsortedList) {
            idMap.put(tc.getId(), tc);
            if (tc.getIsHead() != null && tc.getIsHead()) {
                head = tc;
            }
        }

        if (head == null) {
            Notifier.warn("Warning: ", "No Head found in test cases.");
            return unsortedList;
        }

        List<TestCaseJsonMapper> sortedList = new ArrayList<>();
        TestCaseJsonMapper current = head;

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
            for (TestCaseJsonMapper tc : unsortedList) {
                if (!visited.contains(tc.getId())) {
                    sortedList.add(tc);
                }
            }
        }

        return sortedList;
    }
}
