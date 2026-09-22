/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.model.Priority;
import org.testin.model.TestCaseStatus;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.util.Bundle;

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
    @NonNull
    @Builder.Default
    private volatile String order = "";

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
    private volatile List<String> group = new ArrayList<>();

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
    private volatile ZonedDateTime updatedAt = Config.NOT_EXECUTED;

    @NonNull
    @Builder.Default
    private volatile String module = "";

    @NonNull
    @Builder.Default
    private volatile String testData = "";

    @NonNull
    @Builder.Default
    private volatile String preConditions = "";

    public static @NotNull TestCaseDto deleted(final @NotNull UUID id) {
        return TestCaseDto.builder()
                .id(id)
                .description(Bundle.message("testcase.deleted", id))
                .build();
    }

    public void stampCreated(final @NotNull String tester) {
        createdBy = tester;
        createdAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        updatedBy = "";
        updatedAt = Config.NOT_EXECUTED;
    }

    public void touch(final @NotNull String tester) {
        updatedBy = tester;
        updatedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public void takeAuditOf(final @NotNull TestCaseDto other) {
        createdBy = other.createdBy;
        createdAt = other.createdAt;
        updatedBy = other.updatedBy;
        updatedAt = other.updatedAt;
    }
}