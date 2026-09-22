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

public class TestDataParserRefusalTest {

    @Test
    public void aPriorityItCannotReadIsRefused() {
        assertTrue(TestDataParser.priority("Urgent", Priority.HIGH).isEmpty(),
                "answering P3 is how 200 imported cases all became the lowest priority");
    }

    @Test
    public void aBlankPriorityKeepsWhatTheTestCaseHad() {
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

    @Test
    public void theNoGroupWordIsNoGroupsEither() {
        assertTrue(TestDataParser.groups(Groups.NONE).orElseThrow().isEmpty());
    }
}
