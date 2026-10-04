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

package org.testin.editor.grid;

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.TestCaseEditorAttributes;
import org.testin.testrun.TestRunEditorAttributes;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class GridRowsTest {
    private static final @NotNull TestCaseDto LOGIN = TestCaseDto.builder().id(UUID.randomUUID()).description("Log in with a valid user").build();
    private static final @NotNull TestCaseDto LOGOUT = TestCaseDto.builder().id(UUID.randomUUID()).description("Log out").build();

    private static int position(final @NotNull TestCaseDto tc) {
        return tc == LOGIN ? 7 : 8;
    }

    // Rule-EDITOR-PANEL-020
    @Test
    public void eachTestCaseIsOneRowWithItsPlaceInTheOrderColumn() {
        final @NotNull List<String[]> rows = GridRows.ofTestCases(List.of(LOGIN, LOGOUT), GridRowsTest::position);

        assertEquals(rows.size(), 2, "the test cases are not one row each");
        assertEquals(rows.getFirst().length, TestCaseEditorAttributes.values().length, "a row does not hold one cell per column");
        assertEquals(rows.getFirst()[TestCaseEditorAttributes.ORDER.ordinal()], "7", "the order column does not show the test case's place");
        assertEquals(rows.get(1)[TestCaseEditorAttributes.DESCRIPTION.ordinal()], "Log out", "the description column does not show the description");
    }

    // Rule-EDITOR-PANEL-020
    @Test
    public void aTestCaseWithNoRunItemYetIsStillARowShowingTheTestCase() {
        final @NotNull List<String[]> rows = GridRows.ofRunItems(List.of(LOGIN), Map.of(), GridRowsTest::position);

        assertEquals(rows.size(), 1, "a test case with no run item yet has no row");
        assertEquals(rows.getFirst().length, TestRunEditorAttributes.values().length, "a row does not hold one cell per column");
        assertEquals(rows.getFirst()[TestRunEditorAttributes.ORDER.ordinal()], "7", "the order column does not show the test case's place");
        assertEquals(rows.getFirst()[TestRunEditorAttributes.DESCRIPTION.ordinal()], "Log in with a valid user", "the row does not show the test case it stands for");
    }
}
