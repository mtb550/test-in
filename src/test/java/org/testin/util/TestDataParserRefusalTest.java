package org.testin.util;

import org.testin.model.Group;
import org.testin.model.Priority;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * One answer to a value Testin cannot read (#204, #264).
 * <p>
 * Four columns answered a typo four different ways, and all four in silence: a
 * priority became the lowest, a group was dropped from the list, a date became
 * blank, a status kept whatever the row had. Two of those changed data nobody
 * asked to change - which is how a file of 200 test cases whose priority column
 * reads High, Medium and Low arrived as 200 at the lowest priority with no
 * warning.
 * <p>
 * What is pinned here is the property rather than the mechanism: <b>unreadable
 * is refused, and blank is a value</b>. The dates have their own file; this one
 * covers the two that used to invent an answer.
 */
public class TestDataParserRefusalTest {

    @Test
    public void aPriorityItCannotReadIsRefused() {
        assertTrue(TestDataParser.priority("Urgent", Priority.HIGH).isEmpty(),
                "answering P3 is how 200 imported cases all became the lowest priority");
    }

    @Test
    public void aBlankPriorityKeepsWhatTheCaseHad() {
        assertEquals(TestDataParser.priority("  ", Priority.HIGH).orElseThrow(), Priority.HIGH,
                "a priority has no empty form, so a blank cell says nothing rather than saying P3");
    }

    @Test
    public void aPriorityIsReadByItsLabelAndByItsName() {
        assertEquals(TestDataParser.priority(Priority.HIGH.getLabel(), Priority.LOW).orElseThrow(), Priority.HIGH,
                "the label is what the column shows, so it is what a tester retypes");
        assertEquals(TestDataParser.priority("high", Priority.LOW).orElseThrow(), Priority.HIGH,
                "a sheet exported before the labels became P1 to P3 still says High");
    }

    /**
     * The cell is one value the tester typed, so it is kept or refused as one.
     * Keeping the half it understood is how a tester who mistyped one group in a
     * list of four got three back and no sign the fourth had gone.
     */
    @Test
    public void oneGroupItCannotReadRefusesTheWholeCell() {
        assertTrue(TestDataParser.groups(Group.REGRESSION.name() + ", Nonsense").isEmpty(),
                "half a list is not what the tester typed");
    }

    @Test
    public void groupsItCanReadAreTaken() {
        assertEquals(TestDataParser.groups(Group.REGRESSION.name()).orElseThrow(), List.of(Group.REGRESSION));
    }

    @Test
    public void aBlankCellIsNoGroupsAtAll() {
        assertTrue(TestDataParser.groups("   ").orElseThrow().isEmpty(),
                "groups have an empty form, so a blank cell clears them rather than being refused");
    }
}
