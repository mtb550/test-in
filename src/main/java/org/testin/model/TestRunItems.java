package org.testin.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    /**
     * The test case this result belongs to, wired by the run editor after the
     * run JSON is read - {@code @JsonIgnore}, so it is never deserialized.
     * <p>
     * Every row the editor loads gets one, including a result whose test case
     * has been deleted since the run: that row is wired to
     * {@link TestCaseDto#deleted}, so the attributes that render a row can rely
     * on it. Null only before the editor has wired it - a dialog opened for a
     * raw run item read straight from the file.
     */
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Nullable
    private TestCaseDto tc;

    @NotNull
    private UUID id;
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
    /**
     * When the verdict was given; {@link Config#NOT_EXECUTED} until there is one.
     * It used to default to "now", so every case in a freshly built run already
     * carried a plausible execution time before anyone had run anything.
     */
    @NotNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime executedAt = Config.NOT_EXECUTED;
    @NotNull
    @Builder.Default
    private String stacktrace = "";

    /**
     * True when the test case behind this result has been deleted since the run.
     * <p>
     * The row is drawn from what the run recorded and takes nothing new: a
     * verdict means "we ran it and this is what happened", and a case that is
     * gone cannot be run again. Asked by name so the verdict path, the details
     * editor and the execution walker all ask the same question.
     */
    @JsonIgnore
    public boolean isRemoved() {
        return status == TestStatus.REMOVED;
    }

    /**
     * How long the case took, as the framework that ran it measured.
     * <p>
     * It overrides whatever the editor's own clock counted, because the two are
     * not the same measurement and the framework's is the true one. That clock
     * ticks once a second and truncates, so it reads a case that took 84ms as
     * zero and one that finished before its first tick as never having been
     * timed at all - it exists to time a tester reading a case by hand, where a
     * second either way is nothing.
     * <p>
     * Zero means nothing was measured, so a report that carries no duration
     * leaves what is already there alone. The one check lives here rather than
     * at the call site, and every caller records unconditionally.
     */
    public void recordDuration(final @NotNull Duration measured) {
        if (measured.isZero()) return;

        duration = measured;
    }

    /**
     * Records a tester's verdict: the status, when it was reached, and by whom.
     * <p>
     * Passing clears everything a failure described - the bug severity and
     * priority, the actual result, and the stacktrace. All four exist only to
     * explain why a case is not passing, so a passing case cannot legitimately
     * carry any of them, and they would otherwise survive into the run JSON and
     * into every report generated from it.
     * <p>
     * The clearing keys on the new status alone, not on the one it replaces: a
     * case can collect failure details, be moved to Blocked, and then pass, and
     * the details are just as stale for having taken the long way round.
     * <p>
     * It lives here rather than at the three call sites in {@code RunStatusService}
     * that used to set these fields by hand, so the clearing cannot be bypassed
     * by whichever path applies the status.
     */
    public void recordVerdict(final @NotNull TestStatus next, final @NotNull String tester) {
        if (clears(next)) FailureDetail.clearAll(this);

        status = next;
        executedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        executedBy = tester;
    }

    /**
     * Whether this verdict erases what a failure recorded. Passing does; the
     * other two do not, because a case that is blocked or failing still has
     * something to explain.
     */
    private static boolean clears(final @NotNull TestStatus next) {
        return next == TestStatus.PASSED;
    }

    /**
     * What recording this verdict would erase, in the tester's words, and empty
     * when it would erase nothing (#74).
     * <p>
     * Invisible until the run grid could be typed into: a tester writes a
     * paragraph into the Actual Result cell, presses P, and watches it vanish.
     * So the verdict asks first, and asking means knowing what is at stake -
     * which is this, and which is the same list {@link #recordVerdict} clears,
     * declared once in {@link FailureDetail}.
     */
    public @NotNull List<String> wouldClear(final @NotNull TestStatus next) {
        return clears(next) ? FailureDetail.filledIn(this) : List.of();
    }

    /**
     * The test case, for the paths where it may not be there: a dialog opened on
     * a run item whose case is no longer in the test set. The rendering paths
     * ask {@link #requireTc()} instead, which states the invariant they rely on.
     */
    public @NotNull Optional<TestCaseDto> testCase() {
        return Optional.ofNullable(tc);
    }

    /**
     * The test case, for the rendering path, where it is always present.
     * <p>
     * {@code RunEditor} skips run items whose test case has been deleted and
     * assigns {@code tc} to every one it keeps, so an item that reaches a
     * renderer or a grid row has one. This states that invariant where it is
     * relied on, instead of unchecked reads that look like oversights. If it
     * ever fails, it fails by name rather than as an NPE inside a Swing paint.
     *
     * @throws IllegalStateException if called on an item the editor filtered out
     */
    public @NotNull TestCaseDto requireTc() {
        return Optional.ofNullable(tc).orElseThrow(() -> new IllegalStateException(
                "Run item " + id + " has no test case; it should not have reached the editor"));
    }
}
