package org.testin.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.*;
import org.testin.model.dto.dirs.TestSetDirectoryDto;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.testin.util.Tools;

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

    /**
     * The empty timestamp until the case is edited for the first time: a case
     * nobody has changed has no modification date, and every reader gets a blank
     * from {@link org.testin.util.Tools#formatDate} rather than asking whether it is set.
     */
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

    @JsonIgnore
    @NonNull
    @Builder.Default
    private volatile RunStatus tempStatus = RunStatus.IDLE;

    @JsonIgnore
    @NonNull
    @Builder.Default
    private volatile String tempError = "";

    /**
     * The stand-in for a case a run refers to and the index no longer has.
     * <p>
     * A run records what was executed, and deleting a test case afterwards does
     * not un-execute it. The row therefore stays, carrying the verdict, actual
     * result and timings the run wrote against it, and says plainly that the
     * case itself is gone - the run used to drop the row with only a log line,
     * so a run built with twelve cases quietly showed eleven while its file
     * still held twelve.
     * <p>
     * The id is kept because it is the only identity left: two deleted cases in
     * one run are otherwise the same row twice.
     */
    public static @NotNull TestCaseDto deleted(final @NotNull UUID id) {
        return TestCaseDto.builder()
                .id(id)
                .description("Deleted test case (" + id + ")")
                .build();
    }

    /**
     * Fills the creation audit, and leaves the modification pair empty: a case
     * that has just been made has not been changed by anyone yet. Called once, by
     * the write path, the first time this case is saved.
     */
    public void stampCreated(final @NotNull String tester) {
        createdBy = tester;
        createdAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        updatedBy = "";
        updatedAt = Config.NOT_EXECUTED;
    }

    /**
     * Records a change by the given tester, now. The creation pair is never
     * touched again after {@link #stampCreated}.
     */
    public void touch(final @NotNull String tester) {
        updatedBy = tester;
        updatedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}