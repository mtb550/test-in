package org.testin.editor;

import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testng.annotations.Test;

import javax.swing.JComponent;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The two rules a badge cannot be allowed to break silently (#79).
 * <p>
 * A pill draws text on a background chosen by the value it shows, so the two can
 * disagree: white on a yellow severity says nothing at all, and the failure is
 * invisible to every test that only asks what the badge says. And a case that
 * never failed has no severity and no bug priority, so it must draw no pill
 * rather than an empty one.
 * <p>
 * Neither test builds a badge: constructing one needs the platform's fonts. They
 * pin the two decisions instead - which text colour a background earns, and
 * whether a value draws at all.
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
        final List<JComponent> badges = new ArrayList<>();

        Shared.addBadge(badges, "Bug Severity", BugSeverity.EMPTY.getName(), BugSeverity.EMPTY.getColor());
        Shared.addBadge(badges, "Bug Priority", BugPriority.EMPTY.getName(), BugPriority.EMPTY.getColor());

        assertEquals(badges.size(), 0, "an empty value is not a badge with no text, it is no badge");
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

            assertFalse(severity.getName().isBlank(), severity + " is drawn, so it needs a name");
            assertTrue(names.add(severity.getName()), severity + " repeats another severity's name");
            assertTrue(colours.add(new Color(severity.getColor().getRGB())), severity + " repeats another severity's colour");
        }

        assertEquals(names.size(), 4, "the four severities a tester can choose");
    }
}
