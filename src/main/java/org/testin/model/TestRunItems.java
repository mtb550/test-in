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
    /**
     * Everything the framework said, whole.
     * <p>
     * Never trimmed, sampled or capped on the way to disk. A stacktrace runs to
     * a couple of kilobytes and a run where every case failed would be several
     * times the size of one where none did - which is a decision that has been
     * taken: the run file grows. A truncated stacktrace is missing exactly the
     * frame nobody expected, and the tester who needs it is reading a bug report
     * a week later with no way to get the rest back.
     * <p>
     * What a surface chooses to show of it is a different question, and one the
     * details panel answers by drawing three lines and a link.
     */
    @NotNull
    @Builder.Default
    private String stacktrace = "";

    /**
     * UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219.
     * <p>
     * The screenshots a tester pasted into the error box, by the names of their
     * PNG files beside the run, in the order they were pasted (#50, #313).
     * <p>
     * Names, not pictures: the files are the indexer's to write and remove, and
     * the run file stays a few kilobytes however many screenshots its failures
     * hold. A row with none writes no key, so its file is the same as before
     * screenshots had a place of their own.
     */
    @NotNull
    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> screenshots = List.of();

    /**
     * The GitHub issue this failure was reported as, and empty until one is
     * (#28).
     * <p>
     * Written by Report Bug from the address {@code gh} printed, and by nothing
     * else. A pass clears it with the rest of what a failure recorded - it is one
     * of the {@link FailureDetail}s - and an automated failure leaves it alone,
     * because the same run item failing again in the same run is most often the
     * same bug.
     */
    @NotNull
    @Builder.Default
    private String bugIssueUrl = "";

    /**
     * UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126.
     * <p>
     * Whether the test case behind this result is no longer indexed. Decided by
     * the indexer each time it hands the run out, and never written: the file
     * keeps the verdict the run recorded, so deleting a test case cannot change
     * what an executed run found (#66, finding 110).
     */
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private boolean removed;

    /**
     * True when the test case behind this result has been deleted since the run.
     * <p>
     * The row is drawn from what the run recorded and takes nothing new: a
     * verdict means "we ran it and this is what happened", and a case that is
     * gone cannot be run again. Asked by name so the verdict path, the details
     * editor and the execution walker all ask the same question.
     * <p>
     * A file written by 2.11.0-alpha or earlier can hold REMOVED as the status
     * itself, where the verdict it replaced is already gone; that reads as
     * removed too.
     */
    @JsonIgnore
    public boolean isRemoved() {
        return removed || status == TestStatus.REMOVED;
    }

    /**
     * UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-126.
     * <p>
     * The status a tester sees and every figure counts: Removed for a result
     * whose test case is gone, and the stored status otherwise. Everything that
     * draws, filters or counts a result asks this, so a removed row is never
     * counted as the verdict its file still holds and stays outside the pass
     * rate.
     */
    public @NotNull TestStatus shownStatus() {
        return isRemoved() ? TestStatus.REMOVED : status;
    }

    /**
     * The issue this failure was reported as, and empty when it was not (#28).
     * <p>
     * Every reader asks here - the Details tab, Report Bug and the four reports -
     * so none of them decides for itself what an unset link looks like.
     */
    public @NotNull Optional<String> bugIssue() {
        return bugIssueUrl.isBlank() ? Optional.empty() : Optional.of(bugIssueUrl);
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
     * Whether this row is finished with.
     * <p>
     * A verdict is finished with. So is a case that was deleted from the test
     * set: the run keeps what it recorded about it, and it can never be run
     * again, so waiting for it would be waiting forever.
     * <p>
     * Everything else - Pending, Untested - is a case the run is still expecting
     * something about.
     */
    @JsonIgnore
    public boolean isJudged() {
        return status.isVerdict() || isRemoved();
    }

    /**
     * Records a tester's verdict: the status, when it was reached, and by whom.
     * <p>
     * Passing clears everything a failure described - the bug severity and
     * priority, the actual result, the stacktrace, the screenshots, and the
     * bug issue it was reported as. All six exist only to explain why a case is
     * not passing, so a passing case cannot legitimately carry any of them, and
     * they would
     * otherwise survive into the run JSON and into every report generated from
     * it.
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
     * <p>
     * A failure the automation reports clears too, before it writes its own, and
     * {@link Failure#wouldClear} says what. A verdict given by hand carries
     * {@link Failure#NONE}, so the question the keyboard asks is unchanged (#50).
     */
    public @NotNull List<String> wouldClear(final @NotNull TestStatus next, final @NotNull Failure failure) {
        return clears(next) ? FailureDetail.filledIn(this) : failure.wouldClear(this);
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
     * {@code RunEditor} assigns {@code tc} to every run item it loads - one whose
     * test case has been deleted since the run is wired to
     * {@link TestCaseDto#deleted} - so an item that reaches a renderer or a grid
     * row has one. This states that invariant where it is relied on, instead of
     * unchecked reads that look like oversights. If it ever fails, it fails by
     * name rather than as an NPE inside a Swing paint.
     *
     * @throws IllegalStateException if called on an item the editor has not wired
     */
    public @NotNull TestCaseDto requireTc() {
        return Optional.ofNullable(tc).orElseThrow(() -> new IllegalStateException(
                "Run item " + id + " has no test case; the run editor wires one to every item it loads, so this item was read without being wired"));
    }
}
