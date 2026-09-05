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
     * A case is minutes and seconds, and grows an hours field only if one case
     * somehow runs that long.
     */
    @Test
    public void aCaseClockIsMinutesAndSeconds() {
        assertEquals(Display.formatCaseClock(Duration.ofSeconds(45)), "00:45");
        assertEquals(Display.formatCaseClock(Duration.ofMinutes(3).plusSeconds(7)), "03:07");
        assertEquals(Display.formatCaseClock(Duration.ofHours(2).plusMinutes(5)), "2:05:00");
    }

    /**
     * A running clock never blinks out. Zero on a case in front of a tester means
     * they have just arrived at it, not that nothing was measured.
     */
    @Test
    public void aCaseClockAlwaysShowsANumber() {
        assertEquals(Display.formatCaseClock(Duration.ZERO), "00:00");
        assertEquals(Display.formatCaseClock(Duration.ofMillis(84)), "00:00");
    }

    /**
     * A run carries its hours from the start rather than growing the field at
     * 01:00:00 - and in light mode that is also what keeps it the larger-looking
     * of two unlabelled clocks.
     */
    @Test
    public void aRunClockAlwaysCarriesItsHours() {
        assertEquals(Display.formatRunClock(Duration.ofSeconds(45)), "00:00:45");
        assertEquals(Display.formatRunClock(Duration.ofMinutes(3).plusSeconds(7)), "00:03:07");
        assertEquals(Display.formatRunClock(Duration.ofHours(2).plusMinutes(5)), "02:05:00");
    }

    @Test
    public void aRunNobodyStartedShowsNothing() {
        assertEquals(Display.formatRunClock(Duration.ZERO), "", "the status bar hides the label rather than showing zero");
    }

    /**
     * The recorded value is the case clock plus one guard, and the guard is the
     * reason it is a separate method.
     */
    @Test
    public void aRecordedDurationReadsAsTheCaseClock() {
        assertEquals(Display.formatDuration(Duration.ofSeconds(45)), "00:45");
        assertEquals(Display.formatDuration(Duration.ofMinutes(3).plusSeconds(7)), "03:07");
        assertEquals(Display.formatDuration(Duration.ofHours(2).plusMinutes(5)), "2:05:00");
    }

    @Test
    public void nothingMeasuredShowsNothing() {
        assertEquals(Display.formatDuration(Duration.ZERO), "", "a case nobody ran has no duration line at all");
    }

    /**
     * Milliseconds are measured and stored and never drawn. This used to print
     * "84ms" and a ".400" tail; a fast automated case now reads 00:00 and its
     * real figure stays in the file.
     */
    @Test
    public void millisecondsAreNeverShown() {
        assertEquals(Display.formatDuration(Duration.ofMillis(84)), "00:00");
        assertEquals(Display.formatDuration(Duration.ofMillis(1400)), "00:01");
        assertEquals(Display.formatDuration(Duration.ofSeconds(45).plusMillis(237)), "00:45");
    }
}
