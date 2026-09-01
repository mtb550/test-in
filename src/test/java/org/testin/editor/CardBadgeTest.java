package org.testin.editor;

import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.Priority;
import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * The two rules a badge cannot be allowed to break silently (#79).
 * <p>
 * A pill draws text on a background chosen by the value it shows, so the two can
 * disagree: white on a yellow severity says nothing at all, and the failure is
 * invisible to every test that only asks what the badge says. And a case that
 * never failed has no severity and no bug priority, so it must draw no pill
 * rather than an empty one.
 * <p>
 * Neither test draws a pill: painting one needs the platform's fonts. They pin
 * the two decisions instead - which text colour a background earns, and whether
 * a value becomes a badge at all.
 */
public class CardBadgeTest {

    @Test
    public void aLightBackgroundEarnsDarkText() {
        assertTrue(Shared.isLight(Color.YELLOW), "yellow with white text is the failure this exists to stop");
        assertTrue(Shared.isLight(Color.ORANGE), "orange is light enough to lose white text");
        assertTrue(Shared.isLight(Color.GREEN), "pure green is brighter than it looks");
        assertTrue(Shared.isLight(Color.GRAY.brighter()), "the test case's Low priority, marginal under white");
    }

    @Test
    public void aDarkBackgroundKeepsWhiteText() {
        assertFalse(Shared.isLight(Color.RED), "a blocker is deep red");
        assertFalse(Shared.isLight(Color.BLUE), "blue is dark however bright the channel is");
        assertFalse(Shared.isLight(Color.DARK_GRAY), "the group badge");
    }

    /**
     * Green is the one that surprises: it is the brightest channel to the eye,
     * so a pure green pill reads lighter than a pure blue one even though blue's
     * channel is at the same value.
     */
    @Test
    public void brightnessIsWhatTheEyeSeesNotWhatTheChannelSays() {
        assertTrue(Shared.isLight(new Color(0, 255, 0)));
        assertFalse(Shared.isLight(new Color(0, 0, 255)));
    }

    @Test
    public void aCaseThatNeverFailedDrawsNoPill() {
        final List<Shared.Badge> badges = new ArrayList<>();

        Shared.addBugBadge(badges, BugSeverity.EMPTY, BugPriority.EMPTY);

        assertEquals(badges.size(), 0, "an empty value is not a badge with no text, it is no badge");
    }

    /**
     * Half a pair is not a thing to draw. The two are set together by the
     * failure dialog, so one without the other is a case somebody stopped
     * halfway through describing - and a lone half reads as the other field.
     */
    @Test
    public void halfAPairIsNoPair() {
        final List<Shared.Badge> withoutPriority = new ArrayList<>();
        Shared.addBugBadge(withoutPriority, BugSeverity.MAJOR, BugPriority.EMPTY);
        assertEquals(withoutPriority.size(), 0, "a severity with no bug priority draws nothing");

        final List<Shared.Badge> withoutSeverity = new ArrayList<>();
        Shared.addBugBadge(withoutSeverity, BugSeverity.EMPTY, BugPriority.HIGH);
        assertEquals(withoutSeverity.size(), 0, "a bug priority with no severity draws nothing");
    }

    /**
     * Both halves ask for it and one badge comes out. Severity and bug priority
     * are two toolbar attributes drawing one object, so whichever is ticked
     * draws it and the second finds it already there (#89).
     */
    @Test
    public void bothHalvesAskAndOneBadgeIsDrawn() {
        final List<Shared.Badge> badges = new ArrayList<>();

        Shared.addBugBadge(badges, BugSeverity.MAJOR, BugPriority.HIGH);
        Shared.addBugBadge(badges, BugSeverity.MAJOR, BugPriority.HIGH);

        assertEquals(badges.size(), 1, "the pair is one badge however many of its halves ask for it");
        assertTrue(badges.getFirst() instanceof Shared.Pill pill && pill.text().equals("Major / High"),
                "both facts, one badge, severity first");
    }

    /**
     * Low is what a case is unless somebody said otherwise, so a Low pill says
     * on almost every row what the absence of a pill already says (#89).
     */
    @Test
    public void lowPriorityDrawsNoPill() {
        final List<Shared.Badge> badges = new ArrayList<>();

        Shared.addPriorityBadge(badges, new TestCaseDto().setPriority(Priority.LOW));
        assertEquals(badges.size(), 0, "Low is the default, and the default needs no badge");

        Shared.addPriorityBadge(badges, new TestCaseDto().setPriority(Priority.HIGH));
        Shared.addPriorityBadge(badges, new TestCaseDto().setPriority(Priority.MEDIUM));
        assertEquals(badges.size(), 2, "a priority somebody chose is still drawn");
    }

    /**
     * With the caption gone, color is the only thing keeping the three apart -
     * so the three have to be three.
     */
    @Test
    public void theThreePrioritiesAreThreeColours() {
        final Set<Color> colours = new HashSet<>();

        for (final Priority priority : Priority.values()) {
            assertTrue(colours.add(new Color(priority.getColor().getRGB())), priority + " repeats another priority's colour");
        }

        assertEquals(colours.size(), 3, "three priorities, three colours, no caption to fall back on");
    }

    /**
     * Four severities that mean four different things, so four colours: two
     * sharing one would put the same pill on a blocker and on a suggestion, and
     * the caption is the only thing that would tell them apart.
     */
    @Test
    public void everySeverityThatDrawsHasItsOwnNameAndColour() {
        final Set<String> names = new HashSet<>();
        final Set<Color> colours = new HashSet<>();

        for (final BugSeverity severity : BugSeverity.values()) {
            if (severity == BugSeverity.EMPTY) continue;

            assertFalse(severity.getLabel().isBlank(), severity + " is drawn, so it needs a name");
            assertTrue(names.add(severity.getLabel()), severity + " repeats another severity's name");
            assertTrue(colours.add(new Color(severity.getColor().getRGB())), severity + " repeats another severity's colour");
        }

        assertEquals(names.size(), 4, "the four severities a tester can choose");
    }
}
