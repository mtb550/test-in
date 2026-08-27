package org.testin.java.codegen;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * A tester's text, turned into a Java string literal.
 * <p>
 * This exists because the generated {@code @Test} annotation is built as text,
 * and text that is not a legal Java literal does not fail anywhere: the parser
 * keeps what it was given, the class stops compiling, and every case in that
 * test set stops running with nothing said.
 * <p>
 * <b>The quotes are part of the answer.</b> That is the whole of what these
 * assert, and it is what went wrong: a caller wrapped this in quotes of its own
 * and wrote {@code description = ""verify login""} into a real generated class.
 */
public class JavaLiteralTest {

    @Test
    public void theAnswerCarriesItsOwnQuotes() {
        assertEquals(JavaLiteral.of("verify login"), "\"verify login\"");
    }

    @Test
    public void aQuoteInsideIsEscapedRatherThanEndingTheLiteral() {
        assertEquals(JavaLiteral.of("say \"hello\""), "\"say \\\"hello\\\"\"");
    }

    @Test
    public void aWindowsPathDoesNotBecomeAnIllegalEscape() {
        // clear C:\Users\temp - \U is not a legal Java escape and the class
        // stopped compiling; C:\temp silently became a tab.
        assertEquals(JavaLiteral.of("clear C:\\Users\\temp"), "\"clear C:\\\\Users\\\\temp\"");
    }

    @Test
    public void aNewlineIsEscapedRatherThanWritten() {
        // A description typed across two lines. Written raw it splits the
        // literal in half and the annotation never parses.
        assertEquals(JavaLiteral.of("verify citizen\nlogin successfully"),
                "\"verify citizen\\nlogin successfully\"");
    }

    @Test
    public void punctuationIsCarriedThroughUntouched() {
        // The comma and the bracket are what the old text-splicing ended the
        // value at; nothing here has to treat them specially.
        assertEquals(JavaLiteral.of("Login, then log out (as admin)"),
                "\"Login, then log out (as admin)\"");
    }

    @Test
    public void everyAnswerIsOneQuotedLiteral() {
        for (final String typed : new String[]{"", "plain", "with \"quotes\"", "with\nnewline", "C:\\path"}) {
            final String literal = JavaLiteral.of(typed);

            assertTrue(literal.startsWith("\"") && literal.endsWith("\""),
                    "a caller writes this straight into source, so it opens and closes itself: " + literal);
            assertFalseUnescapedNewline(literal);
        }
    }

    private void assertFalseUnescapedNewline(final String literal) {
        assertTrue(!literal.contains("\n"),
                "a raw newline splits the literal across two lines and the annotation stops parsing: " + literal);
    }
}
