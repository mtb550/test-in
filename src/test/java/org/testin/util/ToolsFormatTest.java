package org.testin.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Display formatting of a test case value (#22).
 * <p>
 * The rule is one line of code and several exceptions, which is exactly the
 * shape that regresses silently: a trailing dot on a URL looks like a typo in
 * the data rather than a bug in the renderer.
 */
public class ToolsFormatTest {

    @Test
    public void plainTextIsCapitalisedAndClosed() {
        assertEquals(Tools.format("login with a valid user"), "Login with a valid user.");
    }

    @Test
    public void alreadyClosedTextIsLeftAlone() {
        assertEquals(Tools.format("Login works."), "Login works.");
        assertEquals(Tools.format("Does it work?"), "Does it work?");
        assertEquals(Tools.format("It works!"), "It works!");
        assertEquals(Tools.format("Steps below:"), "Steps below:");
        assertEquals(Tools.format("First; then second;"), "First; then second;");
    }

    @Test
    public void aUrlOrPathKeepsItsTrailingSlash() {
        assertEquals(Tools.format("https://example.com/"), "Https://example.com/");
    }

    @Test
    public void aParenthesisedNoteIsNotGivenAStop() {
        assertEquals(Tools.format("Run the suite (see README)"), "Run the suite (see README)");
    }

    @Test
    public void blankIsEmpty() {
        assertEquals(Tools.format("   "), "");
        assertEquals(Tools.format(null), "");
    }
}
