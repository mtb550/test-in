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

        Shared.addBugBadge(badges, BugSeverity.EMPTY.getLabel(), BugSeverity.EMPTY.getColor());
        Shared.addBugBadge(badges, BugPriority.EMPTY.getLabel(), BugPriority.EMPTY.getColor());

        assertEquals(badges.size(), 0, "an empty value is not a badge with no text, it is no badge");
    }

    /**
     * One half on its own is that half, because the Details toolbar ticks them
     * separately - severity and bug priority are also two grid columns, so a
     * tester who unticks Bug Priority wants the priority gone and not the badge.
     */
    @Test
    public void oneHalfOnItsOwnIsThatHalf() {
        final List<Shared.Badge> severityOnly = new ArrayList<>();
        Shared.addBugBadge(severityOnly, BugSeverity.MAJOR.getLabel(), BugSeverity.MAJOR.getColor());
        assertEquals(severityOnly.size(), 1);
        assertTrue(severityOnly.getFirst() instanceof Shared.Bug bug && bug.text().equals("Major"));

        final List<Shared.Badge> priorityOnly = new ArrayList<>();
        Shared.addBugBadge(priorityOnly, BugPriority.HIGH.getLabel(), BugPriority.HIGH.getColor());
        assertEquals(priorityOnly.size(), 1);
        assertTrue(priorityOnly.getFirst() instanceof Shared.Bug bug && bug.text().equals("High"),
                "the survivor keeps its own color, which is why BugPriority still declares one");
    }

    /**
     * Both halves add themselves and one badge comes out: the second finds the
     * first and joins it rather than sitting beside it (#89).
     */
    @Test
    public void bothHalvesJoinIntoOneBadge() {
        final List<Shared.Badge> badges = new ArrayList<>();

        Shared.addBugBadge(badges, BugSeverity.MAJOR.getLabel(), BugSeverity.MAJOR.getColor());
        Shared.addBugBadge(badges, BugPriority.HIGH.getLabel(), BugPriority.HIGH.getColor());

        assertEquals(badges.size(), 1, "two halves, one badge");
        assertTrue(badges.getFirst() instanceof Shared.Bug bug && bug.text().equals("Major / High"),
                "severity first, because the enum offers it first");
        assertEquals(((Shared.Bug) badges.getFirst()).color(), BugSeverity.MAJOR.getColor(),
                "the color is the first half's");
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
