package org.testin.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.Accessors;
import org.testin.pojo.Config;
import org.testin.pojo.Group;
import org.testin.pojo.Priority;
import org.testin.pojo.TestCaseStatus;

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
@ToString()
public class TestCaseDto {

    private UUID next;

    private Boolean isHead;

    @NonNull
    @Builder.Default
    @JsonIgnore
    private ArrayList<String> path = new ArrayList<>();

    @NonNull
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @NonNull
    @Builder.Default
    private String description = "";

    @NonNull
    @Builder.Default
    private String expectedResult = "";

    @NonNull
    @Builder.Default
    private TestCaseStatus status = TestCaseStatus.PENDING;

    @NonNull
    @Builder.Default
    private List<String> steps = new ArrayList<>();

    @NonNull
    @Builder.Default
    private Priority priority = Priority.LOW;

    @NonNull
    @Builder.Default
    @JsonIgnore
    private List<String> fqcn = new ArrayList<>();

    @NonNull
    @Builder.Default
    private String reference = "";

    @NonNull
    @Builder.Default
    private List<Group> group = new ArrayList<>();

    @NonNull
    @Builder.Default
    private String createdBy = "";

    @NonNull
    @Builder.Default
    private String updatedBy = "";

    @NonNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime createdAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @NonNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime updatedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @NonNull
    @Builder.Default
    private String module = "";

    @NonNull
    @Builder.Default
    private String testData = "";

    @NonNull
    @Builder.Default
    private String preConditions = "";

    @JsonIgnore
    @NonNull
    @Builder.Default
    private String tempStatus = "";

    @JsonIgnore
    @NonNull
    @Builder.Default
    private String tempError = "";

}