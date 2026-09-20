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

package org.testin.git;

import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.testrun.RunEditorAttributes;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * What the review says about a changed result, the way
 * {@link TestCaseChangeComparatorTest} asks it of a test case (#305).
 * <p>
 * A run's results are one {@code <test case id>.ri} each now, so what a tester
 * reviews before committing is the verdict that changed rather than one line
 * saying a run changed somehow. Every row it shows comes from here.
 */
public class RunItemChangeComparatorTest {

    private static final UUID JUDGED_CASE = UUID.fromString("11111111-1111-4111-8111-111111111101");

    private static final ZonedDateTime EXECUTED_AT =
            ZonedDateTime.of(2026, 9, 20, 11, 30, 0, 0, ZoneId.of("Asia/Riyadh"));

    /**
     * A failure as a tester records one: the verdict, what they saw, what the
     * framework said, how bad it is, who gave it and when, and the picture they
     * pasted.
     */
    private static TestRunItems base() {
        return TestRunItems.builder()
                .id(JUDGED_CASE)
                .status(TestStatus.FAILED)
                .actualResult("The dashboard never opened")
                .stacktrace("java.lang.AssertionError: no dashboard")
                .bugSeverity(BugSeverity.MAJOR)
                .bugPriority(BugPriority.HIGH)
                .bugIssueUrl("https://github.com/mtb550/test-in/issues/305")
                .executedBy("Sara Al-Otaibi")
                .executedAt(EXECUTED_AT)
                .screenshots(List.of("a1b2c.png"))
                .build();
    }

    private static FieldChange onlyChange(final TestRunItems after) {
        final List<FieldChange> changes = RunItemChangeComparator.compare(base(), after);
        assertEquals(changes.size(), 1, "expected exactly one changed field, got " + changes);
        return changes.getFirst();
    }

    /**
     * Everything a result records is compared, and each row is a run item change -
     * the type the revert refuses and the commit stages as a verdict rather than
     * as an edit.
     */
    @Test
    public void comparesEverythingAResultRecords() {
        final List<FieldChange> changes = RunItemChangeComparator.compare(new TestRunItems().setId(JUDGED_CASE), base());

        assertEquals(changes.size(), 9, "a field a tester filled in and the review does not list cannot be read"
                + " before committing it: " + changes);
        assertTrue(changes.stream().allMatch(change -> change.changeType() == ChangeType.CHANGE_RUN_ITEM),
                "every row of a changed result is a result change, whatever field it is about: " + changes);
    }

    /**
     * A verdict reads as its label on both sides, the way the test case editor's
     * status does, rather than as the constant's name in capitals.
     */
    @Test
    public void aVerdictChangeShowsTheLabels() {
        final FieldChange change = onlyChange(base().setStatus(TestStatus.PASSED));

        assertEquals(change.fieldName(), RunEditorAttributes.RUN_STATUS.getName());
        assertEquals(change.oldValue(), TestStatus.FAILED.getLabel());
        assertEquals(change.newValue(), TestStatus.PASSED.getLabel());
    }

    /**
     * The fallback row, and the reason it exists: the commit stages only what the
     * review lists, so a result that changed with nothing this reads different -
     * a duration, a field it does not compare - still has to produce a row the
     * tester can select. Without one, a {@code .ri} Git calls modified would be a
     * change the plugin could never commit (#66).
     */
    @Test
    public void aResultThatChangedNothingThisReadsStillGetsASelectableRow() {
        final FieldChange change = onlyChange(base().setDuration(Duration.ofSeconds(12)));

        assertEquals(change.changeType(), ChangeType.CHANGE_RUN_ITEM);
        assertFalse(change.newValue().isBlank(), "a row with nothing in it says nothing: " + change);

        assertFalse(RunItemChangeComparator.compare(base(), base()).isEmpty(),
                "a result Git reports as changed is always one row at least, or the file it is about cannot be"
                        + " staged at all");
    }

    /**
     * The screenshots are named rather than counted: the file names are what the
     * commit carries beside the result, and a tester reviewing a failure wants to
     * see that a picture arrived with it.
     */
    @Test
    public void theScreenshotsAreNamedRatherThanCounted() {
        final FieldChange change = onlyChange(base().setScreenshots(List.of("a1b2c.png", "d3e4f.png")));

        assertEquals(change.oldValue(), "a1b2c.png");
        assertEquals(change.newValue(), "a1b2c.png, d3e4f.png");
    }

    /**
     * What one line about a result says: the verdict, and what the tester saw when
     * it was not a pass. A passing result is the verdict alone - there is nothing
     * to explain and no dash left dangling after it.
     */
    @Test
    public void oneLineSaysTheVerdictAndWhatWasSeen() {
        assertEquals(RunItemChangeComparator.summary(base()),
                TestStatus.FAILED.getLabel() + " - The dashboard never opened");

        assertEquals(RunItemChangeComparator.summary(base().setStatus(TestStatus.PASSED).setActualResult("")),
                TestStatus.PASSED.getLabel());
    }
}
