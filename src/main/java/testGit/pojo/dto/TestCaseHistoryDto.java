package testGit.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TestCaseHistoryDto {
    private String timestamp;

    private String changeSummary;
}
