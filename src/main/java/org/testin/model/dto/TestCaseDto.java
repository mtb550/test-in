package org.testin.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.Accessors;
import org.testin.enums.Group;
import org.testin.enums.RunStatus;
import org.testin.enums.Priority;
import org.testin.enums.TestCaseStatus;
import org.testin.model.Config;
import org.testin.model.dto.dirs.TestSetDirectoryDto;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public final class TestCaseDto {

    private volatile UUID next;

    private volatile Boolean isHead;

    @NonNull
    @Builder.Default
    private volatile UUID id = UUID.randomUUID();

    @NonNull
    @Builder.Default
    private volatile String description = "";

    @NonNull
    @Builder.Default
    private volatile String expectedResult = "";

    @NonNull
    @Builder.Default
    private volatile TestCaseStatus status = TestCaseStatus.PENDING;

    @NonNull
    @Builder.Default
    private volatile List<String> steps = new ArrayList<>();

    @NonNull
    @Builder.Default
    private volatile Priority priority = Priority.LOW;

    @NonNull
    @Builder.Default
    @JsonIgnore
    private volatile TestSetDirectoryDto parent = new TestSetDirectoryDto();

    @NonNull
    @Builder.Default
    private volatile String reference = "";

    @NonNull
    @Builder.Default
    private volatile List<Group> group = new ArrayList<>();

    @NonNull
    @Builder.Default
    private volatile String createdBy = "";

    @NonNull
    @Builder.Default
    private volatile String updatedBy = "";

    @NonNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private volatile ZonedDateTime createdAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @NonNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private volatile ZonedDateTime updatedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @NonNull
    @Builder.Default
    private volatile String module = "";

    @NonNull
    @Builder.Default
    private volatile String testData = "";

    @NonNull
    @Builder.Default
    private volatile String preConditions = "";

    @JsonIgnore
    @NonNull
    @Builder.Default
    private volatile RunStatus tempStatus = RunStatus.IDLE;

    @JsonIgnore
    @NonNull
    @Builder.Default
    private volatile String tempError = "";
}