package testGit.pojo.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import testGit.pojo.TestRunStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Accessors(chain = true)
public class TestRunDto {
    private String runName;

    private String buildNumber;

    private String platform;

    private String language;

    private String browser;

    private String deviceType;

    private TestRunStatus status;

    private LocalDateTime createdAt;

    private List<TestRunItems> results;

    @Getter
    @Setter
    public static class TestRunItems {
        private UUID testCaseId;

        private String project;

        private String status;

        private Duration duration;

        private String executedBy;

        private LocalDateTime executedAt;
    }
}