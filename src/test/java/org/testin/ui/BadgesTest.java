/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.ui;

import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.Priority;
import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class BadgesTest {

    @Test
    public void aLightBackgroundEarnsDarkText() {
        assertTrue(Badges.isLight(Color.YELLOW), "yellow with white text is the failure this exists to stop");
        assertTrue(Badges.isLight(Color.ORANGE), "orange is light enough to lose white text");
        assertTrue(Badges.isLight(Color.GREEN), "pure green is brighter than it looks");
        assertTrue(Badges.isLight(Color.GRAY.brighter()), "the test case's Low priority, marginal under white");
    }

    @Test
    public void aDarkBackgroundKeepsWhiteText() {
        assertFalse(Badges.isLight(Color.RED), "a blocker is deep red");
        assertFalse(Badges.isLight(Color.BLUE), "blue is dark however bright the channel is");
        assertFalse(Badges.isLight(Color.DARK_GRAY), "the group badge");
    }

    @Test
    public void brightnessIsWhatTheEyeSeesNotWhatTheChannelSays() {
        assertTrue(Badges.isLight(new Color(0, 255, 0)));
        assertFalse(Badges.isLight(new Color(0, 0, 255)));
    }

    @Test
    public void aTestCaseThatNeverFailedDrawsNoPill() {
        final List<Badges.Badge> badges = new ArrayList<>();

        Badges.addBugBadge(badges, BugSeverity.EMPTY.getLabel(), BugSeverity.EMPTY.getColor());
        Badges.addBugBadge(badges, BugPriority.EMPTY.getLabel(), BugPriority.EMPTY.getColor());

        assertEquals(badges.size(), 0, "an empty value is not a badge with no text, it is no badge");
    }

    @Test
    public void oneHalfOnItsOwnIsThatHalf() {
        final List<Badges.Badge> severityOnly = new ArrayList<>();
        Badges.addBugBadge(severityOnly, BugSeverity.MAJOR.getLabel(), BugSeverity.MAJOR.getColor());
        assertEquals(severityOnly.size(), 1);
        assertTrue(severityOnly.getFirst() instanceof Badges.Bug bug && bug.text().equals("Major"));

        final List<Badges.Badge> priorityOnly = new ArrayList<>();
        Badges.addBugBadge(priorityOnly, BugPriority.HIGH.getLabel(), BugPriority.HIGH.getColor());
        assertEquals(priorityOnly.size(), 1);
        assertTrue(priorityOnly.getFirst() instanceof Badges.Bug bug && bug.text().equals("High"),
                "the survivor keeps its own color, which is why BugPriority still declares one");
    }

    @Test
    public void bothHalvesJoinIntoOneBadge() {
        final List<Badges.Badge> badges = new ArrayList<>();

        Badges.addBugBadge(badges, BugSeverity.MAJOR.getLabel(), BugSeverity.MAJOR.getColor());
        Badges.addBugBadge(badges, BugPriority.HIGH.getLabel(), BugPriority.HIGH.getColor());

        assertEquals(badges.size(), 1, "two halves, one badge");
        assertTrue(badges.getFirst() instanceof Badges.Bug bug && bug.text().equals("Major / High"),
                "severity first, because the enum offers it first");
        assertEquals(((Badges.Bug) badges.getFirst()).color(), BugSeverity.MAJOR.getColor(),
                "the color is the first half's");
    }

    @Test
    public void lowPriorityDrawsNoPill() {
        final List<Badges.Badge> badges = new ArrayList<>();

        Badges.addPriorityBadge(badges, new TestCaseDto().setPriority(Priority.LOW));
        assertEquals(badges.size(), 0, "Low is the default, and the default needs no badge");

        Badges.addPriorityBadge(badges, new TestCaseDto().setPriority(Priority.HIGH));
        Badges.addPriorityBadge(badges, new TestCaseDto().setPriority(Priority.MEDIUM));
        assertEquals(badges.size(), 2, "a priority somebody chose is still drawn");
    }

    @Test
    public void theThreePrioritiesAreThreeColours() {
        final Set<Color> colors = new HashSet<>();

        for (final Priority priority : Priority.values()) {
            assertTrue(colors.add(new Color(priority.getColor().getRGB())), priority + " repeats another priority's color");
        }

        assertEquals(colors.size(), 3, "three priorities, three colors, no caption to fall back on");
    }

    @Test
    public void everySeverityThatDrawsHasItsOwnNameAndColour() {
        final Set<String> names = new HashSet<>();
        final Set<Color> colors = new HashSet<>();

        for (final BugSeverity severity : BugSeverity.values()) {
            if (severity == BugSeverity.EMPTY) continue;

            assertFalse(severity.getLabel().isBlank(), severity + " is drawn, so it needs a name");
            assertTrue(names.add(severity.getLabel()), severity + " repeats another severity's name");
            assertTrue(colors.add(new Color(severity.getColor().getRGB())), severity + " repeats another severity's color");
        }

        assertEquals(names.size(), 4, "the four severities a tester can choose");
    }
}
