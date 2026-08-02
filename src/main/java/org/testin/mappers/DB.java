package org.testin.mappers;

import org.testin.mappers.dto.TestCaseHistoryDto;

import java.util.ArrayList;
import java.util.List;

/// TODO:  to be removed
public class DB {

    public static List<TestCaseHistoryDto> loadTestCaseHistory() {
        List<TestCaseHistoryDto> history = new ArrayList<>();
        history.add(new TestCaseHistoryDto("2024-03-01", "Created test case"));
        history.add(new TestCaseHistoryDto("2024-03-15", "Updated expected result"));
        return history;
    }

}
