package org.testin.testCase.updateDialog.bulk;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

/**
 * The escaping the bulk editors write their JSON in (#64).
 * <p>
 * This is the highest-stakes logic in those dialogs: it decides what goes back
 * into storage. The formatting rule in CLAUDE.md is that stored JSON is
 * byte-identical to what the tester typed, so a value nobody touched has to
 * survive the round trip unchanged - and the dialogs rely on that, because they
 * compare the escaped original against the editor text to decide whether a row
 * was edited at all.
 */
public class BulkJsonEditorTest {

    @Test
    public void plainTextIsUnchanged() {
        assertEquals(BulkJsonEditor.escapeJson("Login with a valid user"), "Login with a valid user");
    }

    @Test
    public void quotesAndBackslashesAreEscapedAndComeBack() {
        final String original = "He said \"go\" then C:\\temp\\file";

        final String escaped = BulkJsonEditor.escapeJson(original);
        assertEquals(escaped, "He said \\\"go\\\" then C:\\\\temp\\\\file");
        assertEquals(BulkJsonEditor.unescapeJson(escaped), original);
    }

    @Test
    public void aBackslashBeforeAQuoteSurvivesTheRoundTrip() {
        // The order the two replacements run in decides this one: unescaping the
        // quote before the backslash would turn \\" into a quote that was never
        // typed.
        final String original = "ends with a backslash \\ then \"quoted\"";

        assertEquals(BulkJsonEditor.unescapeJson(BulkJsonEditor.escapeJson(original)), original);
    }

    /**
     * The one case that is deliberately lossy, and the reason an untouched row is
     * never written back.
     */
    @Test
    public void newlinesFlattenToSpacesAndDoNotComeBack() {
        final String original = "first line\nsecond line";

        final String escaped = BulkJsonEditor.escapeJson(original);
        assertEquals(escaped, "first line second line");
        assertNotEquals(BulkJsonEditor.unescapeJson(escaped), original);
    }

    @Test
    public void carriageReturnsAreDropped() {
        assertEquals(BulkJsonEditor.escapeJson("first\r\nsecond"), "first second");
    }

    @Test
    public void anUntouchedValueComparesEqualToItsEscapedSelf() {
        // How both dialogs decide a row was not edited. If this ever stops
        // holding, every row is written back and multi-line values flatten.
        for (final String value : new String[]{"", "plain", "with \"quotes\"", "with \\ backslash", "trailing "}) {
            final String escaped = BulkJsonEditor.escapeJson(value);
            assertEquals(BulkJsonEditor.escapeJson(BulkJsonEditor.unescapeJson(escaped)), escaped, "for: " + value);
        }
    }

    @Test
    public void nullIsEmptyRatherThanACrash() {
        assertEquals(BulkJsonEditor.escapeJson(null), "");
        assertEquals(BulkJsonEditor.unescapeJson(null), "");
    }
}
