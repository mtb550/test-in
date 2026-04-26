package testGit.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Group;
import testGit.pojo.Priority;

import java.time.ZonedDateTime;
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
public class TestCaseDto {
    @Nullable
    private UUID next;

    @Nullable
    private Boolean isHead;

    @NotNull
    @Builder.Default
    private List<String> path = new ArrayList<>();

    @NotNull
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @NotNull
    @Builder.Default
    private String description = "";

    @NotNull
    @Builder.Default
    private String expectedResult = "";

    @NotNull
    @Builder.Default
    private List<String> steps = new ArrayList<>();

    @NotNull
    @Builder.Default
    private Priority priority = Priority.LOW;

    @NotNull
    @Builder.Default
    private List<String> fqcn = new ArrayList<>();

    @NotNull
    @Builder.Default
    private String reference = "";

    @NotNull
    @Builder.Default
    private List<Group> group = new ArrayList<>();

    @NotNull
    @Builder.Default
    private String createdBy = "";

    @NotNull
    @Builder.Default
    private String updatedBy = "";

    @NotNull
    @Builder.Default
    @Getter(AccessLevel.PRIVATE)
    @JsonFormat(pattern = "EEEE hh:mm a dd.MM.yyyy (z)", locale = "en_US")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @NotNull
    @Builder.Default
    @Getter(AccessLevel.PRIVATE)
    @JsonFormat(pattern = "EEEE hh:mm a dd.MM.yyyy (z)", locale = "en_US")
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @NotNull
    @Builder.Default
    private String module = "";

    @JsonIgnore
    @NotNull
    @Builder.Default
    private String tempStatus = "";

    @JsonIgnore
    @NotNull
    @Builder.Default
    private String tempError = "";

    @JsonIgnore
    @JsonProperty("createAt")
    public String getFormattedCreatedAt() {
        return formatTime(this.createdAt);
    }

    @JsonIgnore
    @JsonProperty("updateAt")
    public String getFormattedUpdatedAt() {
        return formatTime(this.updatedAt);
    }

    private String formatTime(final ZonedDateTime time) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE hh:mm a dd.MM.yyyy (z)", Locale.US);
        return time.format(formatter);
    }

    @JsonProperty("createAt")
    private ZonedDateTime getCreateAtForJson() {
        return this.createdAt;
    }

    @JsonProperty("updateAt")
    private ZonedDateTime getUpdateAtForJson() {
        return this.updatedAt;
    }

}
