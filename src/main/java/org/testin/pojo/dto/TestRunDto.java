package org.testin.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.TestRunItems;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString()
// todo, put uuid for eac test run.
public class TestRunDto {

    @NotNull
    @Builder.Default
    private String runName = "";

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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime createdAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @NotNull
    @Builder.Default
    private List<TestRunItems> results = new ArrayList<>();

}