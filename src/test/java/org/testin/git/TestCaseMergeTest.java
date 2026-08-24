package org.testin.git;

import org.testin.util.Mapper;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The rules a conflicted test case is merged by (#90).
 * <p>
 * These are the whole feature. A conflict in Testin is two testers touching the
 * same test case, and what decides whether the tester is asked one question or
 * seventeen - or none - is entirely here.
 */
public class TestCaseMergeTest {

    private static Mapper mapper() {
        try {
            final Constructor<Mapper> constructor = Mapper.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (final Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * A test case as the plugin writes one, with the fields a merge decides
     * about.
     */
    private static String testCase(final String description, final String expected, final String priority, final String updatedBy, final String updatedAt, final String rank) {
        return """
                {
                  "order" : "%s",
                  "id" : "929b97e9-48c1-47c1-9256-2d54080cb2cb",
                  "description" : "%s",
                  "expectedResult" : "%s",
                  "status" : "PENDING",
                  "steps" : [ ],
                  "priority" : "%s",
                  "createdBy" : "muteb",
                  "updatedBy" : "%s",
                  "createdAt" : "Thursday 20-08-2026 At 03:33:24 [Asia/Riyadh]",
                  "updatedAt" : "%s"
                }
                """.formatted(rank, description, expected, priority, updatedBy, updatedAt);
    }

    private static String at(final String time) {
        return "Thursday 20-08-2026 At " + time + " [Asia/Riyadh]";
    }

    /**
     * The case the feature exists for: two testers changed different fields of
     * one test case, which is not a disagreement at all.
     */
    @Test
    public void differentFieldsMergeWithoutAsking() {
        final String base = testCase("sign in", "dashboard opens", "LOW", "", at("09:00:00"), "m");
        final String mine = testCase("a registered user signs in", "dashboard opens", "LOW", "muteb", at("10:00:00"), "m");
        final String theirs = testCase("sign in", "the account dashboard opens", "LOW", "sara", at("11:00:00"), "m");

        final TestCaseMerge.Merge merge = TestCaseMerge.of(mapper(), base, mine, theirs);

        assertTrue(merge.isSettled(), "different fields are not a conflict");
        assertEquals(merge.merged().get("description").asText(), "a registered user signs in");
        assertEquals(merge.merged().get("expectedResult").asText(), "the account dashboard opens");
    }

    /**
     * Both sides stamp the audit fields on every edit, so they conflict even
     * when the testers agreed. The later edit is the answer, and the tester is
     * never shown the question.
     */
    @Test
    public void theAuditStampsAreNeverAQuestion() {
        final String base = testCase("sign in", "", "LOW", "", at("09:00:00"), "m");
        final String mine = testCase("signs in with a valid password", "", "LOW", "muteb", at("10:00:00"), "m");
        final String theirs = testCase("sign in", "the dashboard opens", "LOW", "sara", at("11:30:00"), "m");

        final TestCaseMerge.Merge merge = TestCaseMerge.of(mapper(), base, mine, theirs);

        assertTrue(merge.isSettled());
        assertEquals(merge.merged().get("updatedBy").asText(), "sara", "the later edit names who made it");
        assertEquals(merge.merged().get("updatedAt").asText(), at("11:30:00"));
    }

    /**
     * The one thing that must be asked: both testers rewrote the same field.
     */
    @Test
    public void theSameFieldChangedBothWaysIsAskedAbout() {
        final String base = testCase("sign in", "", "LOW", "", at("09:00:00"), "m");
        final String mine = testCase("a registered user signs in", "", "LOW", "muteb", at("10:00:00"), "m");
        final String theirs = testCase("a known user signs in", "", "LOW", "sara", at("11:00:00"), "m");

        final TestCaseMerge.Merge merge = TestCaseMerge.of(mapper(), base, mine, theirs);

        assertFalse(merge.isSettled());
        assertEquals(merge.questions().size(), 1, "one field, one question - not one per differing line");

        final TestCaseMerge.Question question = merge.questions().getFirst();
        assertEquals(question.field(), "description");
        assertEquals(question.mine(), "a registered user signs in");
        assertEquals(question.theirs(), "a known user signs in");
    }

    /**
     * Answering takes the remote's value, and only for the field answered.
     */
    @Test
    public void answeringTakesTheOtherSideForThatFieldOnly() {
        final String base = testCase("sign in", "opens", "LOW", "", at("09:00:00"), "m");
        final String mine = testCase("mine", "opens", "HIGH", "muteb", at("10:00:00"), "m");
        final String theirs = testCase("theirs", "opens", "LOW", "sara", at("11:00:00"), "m");

        final TestCaseMerge.Merge merge = TestCaseMerge.of(mapper(), base, mine, theirs);
        TestCaseMerge.answer(mapper(), merge.merged(), merge.questions().getFirst(), true, theirs);

        assertEquals(merge.merged().get("description").asText(), "theirs");
        assertEquals(merge.merged().get("priority").asText(), "HIGH", "the priority only I changed is still mine");
    }

    @Test
    public void keepingMyAnswerChangesNothing() {
        final String base = testCase("sign in", "", "LOW", "", at("09:00:00"), "m");
        final String mine = testCase("mine", "", "LOW", "muteb", at("10:00:00"), "m");
        final String theirs = testCase("theirs", "", "LOW", "sara", at("11:00:00"), "m");

        final TestCaseMerge.Merge merge = TestCaseMerge.of(mapper(), base, mine, theirs);
        TestCaseMerge.answer(mapper(), merge.merged(), merge.questions().getFirst(), false, theirs);

        assertEquals(merge.merged().get("description").asText(), "mine");
    }

    /**
     * Both testers moved the same case, to different places. Neither answer
     * means much to the other - the case is one row from where they left it
     * either way - so it is settled rather than asked about.
     */
    @Test
    public void aPositionIsSettledWithoutAsking() {
        final String base = testCase("sign in", "", "LOW", "", at("09:00:00"), "m");
        final String mine = testCase("sign in", "", "LOW", "muteb", at("10:00:00"), "c");
        final String theirs = testCase("sign in", "", "LOW", "sara", at("11:00:00"), "s");

        final TestCaseMerge.Merge merge = TestCaseMerge.of(mapper(), base, mine, theirs);

        assertTrue(merge.isSettled(), "where a case sits is not a question a tester can answer about a merge");
        assertEquals(merge.merged().get("order").asText(), "s");
    }

    /**
     * Two testers who created a case at the same path have no shared past. Every
     * field then reads as set by both, and the ones that differ are asked about.
     */
    @Test
    public void aMissingAncestorAsksAboutWhatDiffers() {
        final String mine = testCase("mine", "opens", "LOW", "muteb", at("10:00:00"), "m");
        final String theirs = testCase("theirs", "opens", "LOW", "sara", at("11:00:00"), "m");

        final TestCaseMerge.Merge merge = TestCaseMerge.of(mapper(), "", mine, theirs);

        assertEquals(merge.questions().size(), 1);
        assertEquals(merge.questions().getFirst().field(), "description");
    }

    /**
     * A side that will not parse says nothing rather than throwing: the other
     * side is then the whole answer, which is what a half-written file during a
     * rebase amounts to.
     */
    @Test
    public void anUnreadableSideIsNotAFailure() {
        final String mine = testCase("mine", "", "LOW", "muteb", at("10:00:00"), "m");

        final TestCaseMerge.Merge merge = TestCaseMerge.of(mapper(), "", mine, "<<<<<<< HEAD not json at all");

        assertTrue(merge.isSettled());
        assertEquals(merge.merged().get("description").asText(), "mine");
    }

    /**
     * Which files this merge can be asked about at all.
     * <p>
     * Both channels ask it now - the Git rebase and the server sync - and that
     * is why it lives here rather than in each of them. Two spellings of the
     * question would mean one channel merging a file the other refused, and a
     * team meeting the difference on the day it costs the most.
     */
    @Test
    public void aTestCaseIsWhatThisCanMerge() {
        assertTrue(TestCaseMerge.isTestCase("Test Cases/Login/6197ec6e.json"));
        assertTrue(TestCaseMerge.isTestCase("Test Cases\\Login\\6197ec6e.json"),
                "Git names paths with slashes and Windows names them with backslashes");
    }

    @Test
    public void aMarkerARunOrAnythingElseIsNot() {
        assertFalse(TestCaseMerge.isTestCase("Test Cases/Login/.ts"), "a marker has no named fields to merge");
        assertFalse(TestCaseMerge.isTestCase(".tp"));
        assertFalse(TestCaseMerge.isTestCase("Test Runs/Cycle 1/Cycle 1.json"),
                "a run records what happened; it is not something two people edit into one");
        assertFalse(TestCaseMerge.isTestCase("Test Cases/Login/notes.txt"));
    }
}
