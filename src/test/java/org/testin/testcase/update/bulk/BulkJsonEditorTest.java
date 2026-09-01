package org.testin.testcase.update.bulk;

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
     * The case that used to be lossy on purpose. A line break became a space,
     * so a tester who edited one row of a multi-line expected result saved it
     * as one line and was told nothing.
     */
    @Test
    public void newlinesSurviveTheRoundTrip() {
        final String original = "first line\nsecond line";

        final String escaped = BulkJsonEditor.escapeJson(original);
        assertEquals(escaped, "first line\\nsecond line", "a line break is written as an escape, on one editor line");
        assertEquals(BulkJsonEditor.unescapeJson(escaped), original);
    }

    /**
     * The pair that decides whether the escape can be unescaped by replacing.
     * It cannot: a backslash followed by an n is written as two backslashes and
     * an n, and a replace looking for the escape finds it inside that.
     */
    @Test
    public void aBackslashFollowedByAnNIsNotALineBreak() {
        final String original = "a windows path C:\\next and a real\nbreak";

        final String escaped = BulkJsonEditor.escapeJson(original);
        assertEquals(BulkJsonEditor.unescapeJson(escaped), original);
        assertNotEquals(BulkJsonEditor.unescapeJson("C:\\\\next"), "C:\\\next");
    }

    @Test
    public void carriageReturnsAreDropped() {
        assertEquals(BulkJsonEditor.escapeJson("first\r\nsecond"), "first\\nsecond");
    }

    @Test
    public void anUntouchedValueComparesEqualToItsEscapedSelf() {
        // How both dialogs decide a row was not edited. If this ever stops
        // holding, every row is written back and multi-line values flatten.
        for (final String value : new String[]{"", "plain", "with \"quotes\"", "with \\ backslash", "trailing ", "two\nlines", "a \\n that is not a break"}) {
            final String escaped = BulkJsonEditor.escapeJson(value);
            assertEquals(BulkJsonEditor.escapeJson(BulkJsonEditor.unescapeJson(escaped)), escaped, "for: " + value);
        }
    }

    @Test
    public void anEmptyValueEscapesToNothing() {
        // A field the tester left blank is "" on the DTO, which is what the
        // escapes now take. This used to pin a null they no longer accept (#71).
        assertEquals(BulkJsonEditor.escapeJson(""), "");
        assertEquals(BulkJsonEditor.unescapeJson(""), "");
    }
}
