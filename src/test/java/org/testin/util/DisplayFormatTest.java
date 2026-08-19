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
public class DisplayFormatTest {

    @Test
    public void plainTextIsCapitalizedAndClosed() {
        assertEquals(Display.format("login with a valid user"), "Login with a valid user.");
    }

    @Test
    public void alreadyClosedTextIsLeftAlone() {
        assertEquals(Display.format("Login works."), "Login works.");
        assertEquals(Display.format("Does it work?"), "Does it work?");
        assertEquals(Display.format("It works!"), "It works!");
        assertEquals(Display.format("Steps below:"), "Steps below:");
        assertEquals(Display.format("First; then second;"), "First; then second;");
    }

    @Test
    public void aUrlOrPathKeepsItsTrailingSlash() {
        assertEquals(Display.format("https://example.com/"), "Https://example.com/");
    }

    @Test
    public void aParenthesisedNoteIsNotGivenAStop() {
        assertEquals(Display.format("Run the suite (see README)"), "Run the suite (see README)");
    }

    @Test
    public void blankIsEmpty() {
        assertEquals(Display.format("   "), "");
        assertEquals(Display.format(null), "");
    }
}
