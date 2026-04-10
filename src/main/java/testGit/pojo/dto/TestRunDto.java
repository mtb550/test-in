package testGit.pojo.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import testGit.pojo.TestRunStatus;

import java.nio.file.Path;
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

    private List<TestCase> testCase;

    private List<TestRunItems> results;

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class TestCase {
        private Path path;

        private List<UUID> uuid;
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class TestRunItems {
        private UUID testCaseId;

        private String project;

        private String status;

        private Duration duration;

        private String executedBy;

        private LocalDateTime executedAt;

        private String stacktrace;

        public String getFormattedDuration() {
            if (duration == null) return "N/A";
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            long seconds = duration.toSecondsPart();
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
    }
}