package testGit.pojo;

import java.util.ArrayList;
import java.util.List;

public class DB {

    public static List<TestCaseHistory> loadTestCaseHistory() {
        List<TestCaseHistory> history = new ArrayList<>();
        history.add(new TestCaseHistory("2024-03-01", "Created test case"));
        history.add(new TestCaseHistory("2024-03-15", "Updated expected result"));
        return history;
    }

}
