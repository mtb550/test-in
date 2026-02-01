package testGit.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TestCaseHistory {
    private String timestamp;
    private String changeSummary;

}
