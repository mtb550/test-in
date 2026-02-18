package testGit.pojo;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class TestRun {
    private String runName;
    private TestRunStatus status;
    private LocalDateTime createdAt;
    private List<TestRunItems> results;

    @Getter
    @Setter
    public static class TestRunItems {
        private UUID testCaseId;   // Pointer to the TestCase file name
        private String status;       // "PASSED", "FAILED", "BLOCKED", "PENDING"
        private Duration duration;     // Execution time
        private String executedBy;
        private LocalDateTime executedAt;
    }
}