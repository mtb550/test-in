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

package org.testin.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestRunItems {
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private @NotNull Optional<TestCaseDto> tc = Optional.empty();

    public @NotNull TestRunItems setTc(final @NotNull TestCaseDto testCase) {
        this.tc = Optional.of(testCase);
        return this;
    }

    @NotNull
    @Builder.Default
    private UUID id = new UUID(0L, 0L);
    @NotNull
    @Builder.Default
    private TestStatus status = TestStatus.PENDING;
    @NotNull
    @Builder.Default
    private String actualResult = "";
    @NotNull
    @Builder.Default
    private BugSeverity bugSeverity = BugSeverity.EMPTY;
    @NotNull
    @Builder.Default
    private BugPriority bugPriority = BugPriority.EMPTY;
    @NotNull
    @Builder.Default
    private Duration duration = Duration.ZERO;
    @NotNull
    @Builder.Default
    private String executedBy = "";
    @NotNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime executedAt = Config.NOT_EXECUTED;
    @NotNull
    @Builder.Default
    private String stacktrace = "";

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    @NotNull
    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> screenshots = List.of();

    @NotNull
    @Builder.Default
    private String bugIssueUrl = "";

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private boolean removed;

    @JsonIgnore
    public boolean isRemoved() {
        return removed || status == TestStatus.REMOVED;
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126
    public @NotNull TestStatus shownStatus() {
        return isRemoved() ? TestStatus.REMOVED : status;
    }

    public @NotNull Optional<String> bugIssue() {
        return bugIssueUrl.isBlank() ? Optional.empty() : Optional.of(bugIssueUrl);
    }

    public void recordDuration(final @NotNull Duration measured) {
        if (measured.isZero()) return;

        duration = measured;
    }

    @JsonIgnore
    public boolean isJudged() {
        return status.isVerdict() || isRemoved();
    }

    public void recordVerdict(final @NotNull TestStatus next, final @NotNull String tester) {
        if (clears(next)) FailureDetail.clearAll(this);

        status = next;
        executedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        executedBy = tester;
    }

    private static boolean clears(final @NotNull TestStatus next) {
        return next == TestStatus.PASSED;
    }

    public @NotNull List<String> wouldClear(final @NotNull TestStatus next, final @NotNull Failure failure) {
        return clears(next) ? FailureDetail.filledIn(this) : failure.wouldClear(this);
    }

    public @NotNull Optional<TestCaseDto> testCase() {
        return tc;
    }

    public @NotNull TestCaseDto shownCase() {
        return tc.orElseGet(() -> TestCaseDto.deleted(id));
    }
}
