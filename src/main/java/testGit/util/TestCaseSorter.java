package testGit.util;

import testGit.pojo.dto.TestCaseDto;

import java.util.*;

public class TestCaseSorter {

    public static List<TestCaseDto> sortTestCases(List<TestCaseDto> unsortedList) {
        if (unsortedList == null || unsortedList.isEmpty()) {
            System.out.println("no test cases");
            return new ArrayList<>();
        }

        Map<String, TestCaseDto> idMap = new HashMap<>();
        TestCaseDto head = null;

        for (TestCaseDto tc : unsortedList) {
            idMap.put(tc.getId(), tc);
            if (tc.getIsHead() != null && tc.getIsHead()) {
                head = tc;
            }
        }

        if (head == null) {
            Notifier.warn("Warning: ", "No Head found in test cases.");
            return unsortedList;
        }

        List<TestCaseDto> sortedList = new ArrayList<>();
        TestCaseDto current = head;

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
            for (TestCaseDto tc : unsortedList) {
                if (!visited.contains(tc.getId())) {
                    sortedList.add(tc);
                }
            }
        }

        return sortedList;
    }
}
