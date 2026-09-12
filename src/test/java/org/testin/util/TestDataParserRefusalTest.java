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

package org.testin.util;

import org.testin.model.Groups;
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
     * A group is a word now, not a constant, so there is nothing left to refuse:
     * a name Testin has never seen is a name the tester is adding (#296). This
     * used to be the one column that could refuse part of a cell, and it kept
     * the half it understood without saying so (#264).
     */
    @Test
    public void aGroupNobodyHasUsedBeforeIsTaken() {
        assertEquals(TestDataParser.groups("Regression, Payments").orElseThrow(), List.of("Regression", "Payments"));
    }

    @Test
    public void aGroupNamedTwiceIsNamedOnce() {
        assertEquals(TestDataParser.groups("Regression, Regression").orElseThrow(), List.of("Regression"),
                "the bulk editor deduplicated and the grid did not, so one cell had two answers (#295)");
    }

    @Test
    public void aBlankCellIsNoGroupsAtAll() {
        assertTrue(TestDataParser.groups("   ").orElseThrow().isEmpty(),
                "groups have an empty form, so a blank cell clears them");
    }

    /**
     * The picker writes the words it draws, so the parser has to read them back.
     * No Group is not a group; it is how a tester says there are none.
     */
    @Test
    public void theNoGroupWordIsNoGroupsEither() {
        assertTrue(TestDataParser.groups(Groups.NONE).orElseThrow().isEmpty());
    }
}
