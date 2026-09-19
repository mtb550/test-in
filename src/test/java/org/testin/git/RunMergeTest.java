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

import org.testin.util.RealMapper;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * What happens when two testers execute one cycle and both push (#305, Q-D, Q-E).
 * <p>
 * Judging different cases is no longer a conflict at all - their verdicts are in
 * different files. What is left is the two things they both write: the same
 * case's result, and the run's own marker. Neither asks the tester anything here,
 * and these are the rules that decide instead.
 */
public class RunMergeTest {

    private static final String BASE_ITEM = """
            {
              "id" : "11111111-1111-4111-8111-111111111101",
              "status" : "PENDING"
            }""";

    private static final String MINE_ITEM = """
            {
              "id" : "11111111-1111-4111-8111-111111111101",
              "status" : "PASSED",
              "actualResult" : "Signed in",
              "executedBy" : "Muteb",
              "executedAt" : "Monday 14-09-2026 At 10:00:00 [Asia/Riyadh]"
            }""";

    private static final String THEIRS_ITEM = """
            {
              "id" : "11111111-1111-4111-8111-111111111101",
              "status" : "FAILED",
              "actualResult" : "The lockout counter never reset",
              "stacktrace" : "boom",
              "executedBy" : "Sara",
              "executedAt" : "Monday 14-09-2026 At 11:30:00 [Asia/Riyadh]"
            }""";

    /**
     * Q-E. A verdict travels whole: the later one wins with everything it
     * recorded, so nobody ends up with a Passed carrying the other tester's
     * stacktrace.
     */
    @Test
    public void theLaterVerdictWinsWhole() {
        final Merge merge = RunItemMerge.of(RealMapper.build(), BASE_ITEM, MINE_ITEM, THEIRS_ITEM);

        assertTrue(merge.isSettled(), "a verdict is settled by rule, never by asking");
        assertEquals(merge.merged().path("status").asText(), "FAILED");
        assertEquals(merge.merged().path("executedBy").asText(), "Sara");
        assertEquals(merge.merged().path("stacktrace").asText(), "boom", "the failure's evidence comes with it");
        assertEquals(merge.settled().size(), 1, "and the tester is told a choice was made: " + merge.settled());
    }

    @Test
    public void theEarlierVerdictLosesWhole() {
        final Merge merge = RunItemMerge.of(RealMapper.build(), BASE_ITEM, THEIRS_ITEM, MINE_ITEM);

        assertEquals(merge.merged().path("status").asText(), "FAILED", "which side is mine does not decide it");
        assertEquals(merge.merged().path("executedBy").asText(), "Sara");
    }

    @Test
    public void twoIdenticalVerdictsAreNoDisagreement() {
        final Merge merge = RunItemMerge.of(RealMapper.build(), BASE_ITEM, MINE_ITEM, MINE_ITEM);

        assertTrue(merge.isSettled());
        assertTrue(merge.settled().isEmpty(), "nothing was decided, because nothing differed");
        assertEquals(merge.merged().path("status").asText(), "PASSED");
    }

    private static final String BASE_MARKER = """
            {
              "createdBy" : "Muteb",
              "createdAt" : "Sunday 13-09-2026 At 09:00:00 [Asia/Riyadh]",
              "status" : "CREATED",
              "configuration" : { "PLATFORM" : "Web" }
            }""";

    private static final String MINE_MARKER = """
            {
              "createdBy" : "Muteb",
              "createdAt" : "Sunday 13-09-2026 At 09:00:00 [Asia/Riyadh]",
              "modifiedBy" : "Muteb",
              "modifiedAt" : "Monday 14-09-2026 At 10:05:00 [Asia/Riyadh]",
              "status" : "IN_PROGRESS",
              "configuration" : { "PLATFORM" : "Web", "BROWSER" : "Chrome" },
              "executionStartedAt" : "Monday 14-09-2026 At 10:00:00 [Asia/Riyadh]",
              "executionEndedAt" : "Monday 14-09-2026 At 10:20:00 [Asia/Riyadh]"
            }""";

    private static final String THEIRS_MARKER = """
            {
              "createdBy" : "Muteb",
              "createdAt" : "Sunday 13-09-2026 At 09:00:00 [Asia/Riyadh]",
              "modifiedBy" : "Sara",
              "modifiedAt" : "Monday 14-09-2026 At 11:40:00 [Asia/Riyadh]",
              "status" : "COMPLETED",
              "configuration" : { "PLATFORM" : "Web", "COMPONENT" : "Storefront" },
              "executionStartedAt" : "Monday 14-09-2026 At 09:30:00 [Asia/Riyadh]",
              "executionEndedAt" : "Monday 14-09-2026 At 11:35:00 [Asia/Riyadh]"
            }""";

    /**
     * Q-D. A cycle two people executed ran from the first thing either of them
     * did to the last, and a run somebody completed is not created again.
     */
    @Test
    public void aRunRanFromTheFirstStartToTheLastStop() {
        final Merge merge = RunMarkerMerge.of(RealMapper.build(), BASE_MARKER, MINE_MARKER, THEIRS_MARKER);

        assertTrue(merge.isSettled(), "none of this is the tester's to answer: " + merge.questions());
        assertEquals(merge.merged().path("executionStartedAt").asText(), "Monday 14-09-2026 At 09:30:00 [Asia/Riyadh]");
        assertEquals(merge.merged().path("executionEndedAt").asText(), "Monday 14-09-2026 At 11:35:00 [Asia/Riyadh]");
        assertEquals(merge.merged().path("status").asText(), "COMPLETED", "the status further along");
        assertEquals(merge.merged().path("modifiedBy").asText(), "Sara", "the audit block takes the later edit");
    }

    /**
     * What the two testers answered merges key by key: a question only one of
     * them answered is not a disagreement.
     */
    @Test
    public void theAnswersTheyEachGaveAreBothKept() {
        final Merge merge = RunMarkerMerge.of(RealMapper.build(), BASE_MARKER, MINE_MARKER, THEIRS_MARKER);

        assertEquals(merge.merged().path("configuration").path("BROWSER").asText(), "Chrome");
        assertEquals(merge.merged().path("configuration").path("COMPONENT").asText(), "Storefront");
        assertEquals(merge.merged().path("configuration").path("PLATFORM").asText(), "Web", "neither touched it");
    }

    /**
     * The one thing left for the tester: a question both of them answered, and
     * differently.
     */
    @Test
    public void onlyAFieldBothChangedIsAskedAbout() {
        final String theirs = THEIRS_MARKER.replace("\"PLATFORM\" : \"Web\"", "\"PLATFORM\" : \"Mobile\"");
        final String mine = MINE_MARKER.replace("\"PLATFORM\" : \"Web\"", "\"PLATFORM\" : \"Desktop\"");

        final Merge merge = RunMarkerMerge.of(RealMapper.build(), BASE_MARKER, mine, theirs);

        assertEquals(merge.questions().size(), 1, "one question, about the one field both answered: " + merge.questions());
        assertEquals(merge.questions().getFirst().field(), "configuration.PLATFORM");
        assertEquals(merge.questions().getFirst().mine(), "Desktop");
        assertEquals(merge.questions().getFirst().theirs(), "Mobile");

        merge.answer(RealMapper.build(), merge.questions().getFirst(), true, theirs);
        assertEquals(merge.merged().path("configuration").path("PLATFORM").asText(), "Mobile",
                "the tester's answer is written where the value lives, inside the configuration");
    }
}
