package org.testin.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;
import org.testin.pojo.Config;
import org.testin.pojo.Group;
import org.testin.pojo.Priority;

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
public class TestCaseDto {

    private UUID next;

    private Boolean isHead;

    @NonNull
    @Builder.Default
    @JsonIgnore
    private List<String> path = new ArrayList<>();

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
    private String status = ""; // todo, to be update to fetch from enum class (Test Case Status)

    @NonNull
    @Builder.Default
    private List<String> steps = new ArrayList<>();

    @NonNull
    @Builder.Default
    private Priority priority = Priority.LOW;

    @NonNull
    @Builder.Default
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
    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime createdAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @NonNull
    @Builder.Default
    @JsonProperty("updatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEEE dd-MM-yyyy 'At' HH:mm:ss '['VV']'", locale = "en_US")
    private ZonedDateTime updatedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @NonNull
    @Builder.Default
    private String module = "";

    @JsonIgnore
    @NonNull
    @Builder.Default
    private String tempStatus = "";

    @JsonIgnore
    @NonNull
    @Builder.Default
    private String tempError = "";

}