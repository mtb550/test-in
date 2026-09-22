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

package org.testin.testcase;

import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class TestCaseOrderTest {

    private static TestCaseDto testCase(final String description, final String rank) {
        return TestCaseDto.builder()
                .id(UUID.randomUUID())
                .description(description)
                .order(rank)
                .createdAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .build();
    }

    @Test
    public void testCasesAreShownInRankOrderWhateverOrderTheyWereRead() {
        final TestCaseDto first = testCase("sign in", "c");
        final TestCaseDto second = testCase("sign out", "m");
        final TestCaseDto third = testCase("a wrong password is refused", "s");

        assertEquals(TestCaseOrder.ordered(List.of(third, first, second)), List.of(first, second, third));
        assertEquals(TestCaseOrder.ordered(List.of(second, third, first)), List.of(first, second, third));
    }

    @Test
    public void aTestCaseWithNoRankIsShownLastRatherThanHidden() {
        final TestCaseDto ranked = testCase("sign in", "c");
        final TestCaseDto arrived = testCase("copied in from somewhere", "");

        final List<TestCaseDto> ordered = TestCaseOrder.ordered(List.of(arrived, ranked));

        assertEquals(ordered.size(), 2);
        assertEquals(ordered, List.of(ranked, arrived));
    }

    @Test
    public void equalRanksStillGiveEveryMachineTheSameOrder() {
        final TestCaseDto mine = testCase("a signed-in user signs out", "s");
        final TestCaseDto theirs = testCase("a locked account cannot sign in", "s");

        assertEquals(TestCaseOrder.ordered(List.of(mine, theirs)), TestCaseOrder.ordered(List.of(theirs, mine)));
    }

    @Test
    public void placingWritesOnlyTheTestCaseThatMoved() {
        final TestCaseDto first = testCase("sign in", "c");
        final TestCaseDto second = testCase("sign out", "m");
        final TestCaseDto third = testCase("a wrong password is refused", "s");

        final List<TestCaseDto> arranged = new ArrayList<>(List.of(first, third, second));
        final List<TestCaseDto> moved = TestCaseOrder.place(arranged);

        assertEquals(moved.size(), 1, "one drag, one file");
        assertEquals(TestCaseOrder.ordered(new ArrayList<>(arranged)), arranged, "and the list now sorts as arranged");
        assertEquals(first.getOrder(), "c", "the case at the top never moved, so its rank is untouched");
    }

    @Test
    public void placingGivesAnUnrankedTestCaseARank() {
        final TestCaseDto ranked = testCase("sign in", "c");
        final TestCaseDto arrived = testCase("copied in from somewhere", "");

        final List<TestCaseDto> moved = TestCaseOrder.place(new ArrayList<>(List.of(ranked, arrived)));

        assertEquals(moved, List.of(arrived));
        assertEquals(ranked.getOrder(), "c");
        assertEquals(TestCaseOrder.ordered(List.of(arrived, ranked)), List.of(ranked, arrived));
    }

    @Test
    public void anEmptySetSortsToNothingRatherThanFailing() {
        assertEquals(TestCaseOrder.ordered(List.of()), List.of());
        assertEquals(TestCaseOrder.place(new ArrayList<>()), List.of());
    }
}
