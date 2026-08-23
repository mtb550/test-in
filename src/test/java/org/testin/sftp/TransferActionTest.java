package org.testin.sftp;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * The whole rule for what a sync does about one file (#94).
 * <p>
 * Written as the table it is. Three hashes go in - what the file was at the last
 * transfer, what it is here, what it is on the server - and exactly one action
 * comes out. Every row below is a situation a tester can actually create, and
 * the comment on each says which one.
 * <p>
 * The rows that matter most are the two deletions and the two delete-against-edit
 * collisions, because those are the ones where being wrong destroys work that
 * has no other copy.
 */
public class TransferActionTest {

    /**
     * A hash is opaque here; only whether two of them are equal matters. Empty
     * means the file is not on that side.
     */
    private static final String OLD = "aaa";
    private static final String MINE = "bbb";
    private static final String THEIRS = "ccc";
    private static final String GONE = "";

    @DataProvider(name = "table")
    public static Object[][] table() {
        return new Object[][]{
                // base, local, remote, expected, the situation
                {GONE, GONE, GONE, TransferAction.NOTHING, "nobody has this file"},
                {OLD, OLD, OLD, TransferAction.NOTHING, "nothing has moved since the last sync"},
                {GONE, MINE, MINE, TransferAction.NOTHING, "both added the identical file"},
                {OLD, MINE, MINE, TransferAction.NOTHING, "both made the same edit"},

                {GONE, MINE, GONE, TransferAction.UPLOAD, "a test case written here since the last sync"},
                {OLD, MINE, OLD, TransferAction.UPLOAD, "edited here, untouched on the server"},

                {GONE, GONE, THEIRS, TransferAction.DOWNLOAD, "a colleague added a test case"},
                {OLD, OLD, THEIRS, TransferAction.DOWNLOAD, "a colleague edited it, this machine did not"},

                {OLD, OLD, GONE, TransferAction.DELETE_LOCAL, "a colleague deleted it; nothing to lose here"},
                {OLD, GONE, OLD, TransferAction.DELETE_REMOTE, "deleted here; the server still has the old one"},

                {GONE, MINE, THEIRS, TransferAction.RESOLVE, "both added the same path, differently"},
                {OLD, MINE, THEIRS, TransferAction.RESOLVE, "both edited it since the last sync"},
                {OLD, GONE, THEIRS, TransferAction.RESOLVE, "deleted here, edited there"},
                {OLD, MINE, GONE, TransferAction.RESOLVE, "edited here, deleted there"},
        };
    }

    @Test(dataProvider = "table")
    public void decidesOneActionPerSituation(final String base, final String local, final String remote,
                                             final TransferAction expected, final String situation) {
        assertEquals(TransferAction.of(base, local, remote), expected, situation);
    }

    /**
     * Nothing falls through.
     * <p>
     * Every combination of three values out of a set that includes "absent"
     * lands on a constant. A gap here would not throw - it would return
     * something, and a sync would act on it.
     */
    @Test
    public void everyCombinationLandsSomewhere() {
        final List<String> values = List.of(GONE, OLD, MINE, THEIRS);

        for (final String base : values) {
            for (final String local : values) {
                for (final String remote : values) {
                    assertNotNull(TransferAction.of(base, local, remote),
                            "no action for base=" + base + " local=" + local + " remote=" + remote);
                }
            }
        }
    }

    /**
     * A sync run twice does nothing the second time.
     * <p>
     * After any action, the baseline becomes what both sides now hold - so
     * feeding that back in must settle. If it did not, a tester would be asked
     * the same question on every sync forever.
     */
    @Test
    public void asyncThatJustRanHasNothingLeftToDo() {
        final List<String> values = List.of(GONE, OLD, MINE, THEIRS);

        for (final String settled : values) {
            assertEquals(TransferAction.of(settled, settled, settled), TransferAction.NOTHING,
                    "a file all three sides agree on is finished");
        }
    }

    /**
     * The direction of a deletion is never guessed.
     * <p>
     * The two deletion actions are only reached when the side that still has the
     * file is holding exactly what was last transferred. If it has changed since,
     * somebody is losing work and the tester decides - which is the pair of
     * RESOLVE rows above.
     */
    @Test
    public void aDeletionOnlyPropagatesWhenTheOtherSideIsUntouched() {
        assertEquals(TransferAction.of(OLD, OLD, GONE), TransferAction.DELETE_LOCAL);
        assertEquals(TransferAction.of(OLD, MINE, GONE), TransferAction.RESOLVE,
                "it was edited here, so deleting it because the server did would lose that edit");

        assertEquals(TransferAction.of(OLD, GONE, OLD), TransferAction.DELETE_REMOTE);
        assertEquals(TransferAction.of(OLD, GONE, THEIRS), TransferAction.RESOLVE,
                "a colleague edited it, so deleting it on the server would lose their work");
    }

    @Test
    public void everyActionSaysWhatItIsInWordsATesterReads() {
        for (final TransferAction action : TransferAction.values()) {
            assertNotNull(action.getCaption());
            assertEquals(action.getCaption().isBlank(), false, action + " has no caption");
        }
    }
}
