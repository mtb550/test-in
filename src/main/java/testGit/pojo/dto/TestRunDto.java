package testGit.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.TestRunStatus;
import testGit.pojo.TestStatus;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestRunDto {

    @NotNull
    @Builder.Default
    private String runName = "";

    @NotNull
    @Builder.Default
    private String buildNumber = "";

    @NotNull
    @Builder.Default
    private String commitId = "";

    @NotNull
    @Builder.Default
    private String platform = "";

    @NotNull
    @Builder.Default
    private String language = "";

    @NotNull
    @Builder.Default
    private String browser = "";

    @NotNull
    @Builder.Default
    private String deviceType = "";

    @NotNull
    @Builder.Default
    private TestRunStatus status = TestRunStatus.CREATED;

    @NotNull
    @Builder.Default
    @Getter(AccessLevel.PRIVATE)
    @JsonFormat(pattern = "EEEE hh:mm a dd.MM.yyyy", locale = "en_US")
    private LocalDateTime createdAt = LocalDateTime.now();

    @NotNull
    @Builder.Default
    private List<TestCase> testCase = new ArrayList<>();

    @NotNull
    @Builder.Default
    private List<TestRunItems> results = new ArrayList<>();

    @JsonIgnore
    @JsonProperty("createAt")
    public String getFormattedCreatedAt() {
        return formatTime(this.createdAt);
    }

    private String formatTime(final LocalDateTime time) {
        if (time == null) return "";
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE hh:mm a dd.MM.yyyy", Locale.US);
        return time.format(formatter);
    }

    @JsonProperty("createAt")
    private LocalDateTime getCreateAtForJson() {
        return this.createdAt;
    }

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TestCase {

        @Nullable
        private Path path;

        @NotNull
        @Builder.Default
        private List<UUID> uuid = new ArrayList<>();
    }

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TestRunItems {
        @JsonIgnore
        @NotNull
        private TestCaseDto testCaseDetails;

        @Nullable
        private UUID testCaseId;

        @NotNull
        @Builder.Default
        private String project = "";

        @NotNull
        @Builder.Default
        private TestStatus status = TestStatus.PENDING;

        @NotNull
        @Builder.Default
        private String actualResult = "";

        @NotNull
        @Builder.Default
        private Duration duration = Duration.ZERO;

        @NotNull
        @Builder.Default
        private String executedBy = "";

        @NotNull
        @Builder.Default
        @Getter(AccessLevel.PRIVATE)
        @JsonFormat(pattern = "EEEE hh:mm a dd.MM.yyyy", locale = "en_US")
        private LocalDateTime executedAt = LocalDateTime.now();

        @NotNull
        @Builder.Default
        private String stacktrace = "";

        @JsonIgnore
        @JsonProperty("executedAtFormatted")
        public String getFormattedExecutedAt() {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE hh:mm a dd.MM.yyyy", Locale.US);
            return this.executedAt.format(formatter);
        }

        @JsonProperty("executedAt")
        private LocalDateTime getExecutedAtForJson() {
            return this.executedAt;
        }
    }
}