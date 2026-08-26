package org.testin.util;

import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

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
        assertEquals(Display.format(""), "");
    }

    /**
     * A dialog naming what it is about to erase reads as a sentence, so the
     * list inside it has to as well - "the actual result, the stacktrace" is a
     * log line, not a warning somebody reads.
     */
    @Test
    public void aListOfThingsReadsAsASentence() {
        assertEquals(Display.andJoin(List.of()), "");
        assertEquals(Display.andJoin(List.of("the actual result")), "the actual result");
        assertEquals(Display.andJoin(List.of("the actual result", "the stacktrace")),
                "the actual result and the stacktrace");
        assertEquals(Display.andJoin(List.of("the actual result", "the stacktrace", "the bug severity")),
                "the actual result, the stacktrace and the bug severity");
    }

    /**
     * The clock used to be the only source and ticked once a second, so
     * HH:MM:SS lost nothing. A test framework measures the method itself and
     * reports in milliseconds, and the old format rendered every fast case as
     * 00:00:00 - a duration that reads as no time at all.
     */
    @Test
    public void aDurationUnderASecondReadsAsMilliseconds() {
        assertEquals(Display.formatDuration(Duration.ofMillis(84)), "84ms");
        assertEquals(Display.formatDuration(Duration.ofMillis(1)), "1ms");
        assertEquals(Display.formatDuration(Duration.ofMillis(999)), "999ms");
    }

    @Test
    public void aSecondAndOverKeepsTheClockFormatATesterAlreadyReads() {
        assertEquals(Display.formatDuration(Duration.ofSeconds(1)), "00:00:01");
        assertEquals(Display.formatDuration(Duration.ofMillis(1400)), "00:00:01", "the seconds format truncates, as it always did");
        assertEquals(Display.formatDuration(Duration.ofMinutes(3).plusSeconds(7)), "00:03:07");
        assertEquals(Display.formatDuration(Duration.ofHours(2).plusMinutes(5)), "02:05:00");
    }

    @Test
    public void nothingMeasuredShowsNothing() {
        assertEquals(Display.formatDuration(Duration.ZERO), "", "a case nobody ran has no duration line at all");
    }
}
