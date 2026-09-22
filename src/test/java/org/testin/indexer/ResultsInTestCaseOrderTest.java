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

package org.testin.indexer;

import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class ResultsInTestCaseOrderTest {

    private static final @NotNull UUID FIRST = UUID.fromString("11111111-1111-4111-8111-111111111101");

    private static final @NotNull UUID SECOND = UUID.fromString("11111111-1111-4111-8111-111111111102");

    private static final @NotNull UUID DELETED_TEST_CASE = UUID.fromString("11111111-1111-4111-8111-111111111103");

    private static @NotNull ScannedProject aProjectHoldingBothTestCases() {
        final @NotNull ScannedProject scanned = new ScannedProject();
        scanned.getTestCasesById().put(FIRST, TestCaseDto.builder().id(FIRST).order("a").build());
        scanned.getTestCasesById().put(SECOND, TestCaseDto.builder().id(SECOND).order("b").build());

        return scanned;
    }

    private static @NotNull TestRunItems result(final @NotNull UUID testCaseId, final @NotNull TestStatus status) {
        return TestRunItems.builder().id(testCaseId).status(status).build();
    }

    private static @NotNull List<UUID> idsOf(final @NotNull List<TestRunItems> results) {
        return results.stream().map(TestRunItems::getId).toList();
    }

    @Test
    public void theResultsComeBackInTheirTestCasesTestSetOrder() {
        final @NotNull List<TestRunItems> asTheFolderListedThem =
                List.of(result(SECOND, TestStatus.FAILED), result(FIRST, TestStatus.PASSED));

        final @NotNull List<TestRunItems> ordered = IndexingScanner.inTestCaseOrder(asTheFolderListedThem, aProjectHoldingBothTestCases());

        assertEquals(idsOf(ordered), List.of(FIRST, SECOND),
                "A run's results are drawn, printed and exported in this order, so it is the order the cases sit"
                        + " in their test sets - never the order the folder happened to list their files in");
    }

    @Test
    public void aResultWhoseTestCaseIsGoneComesLastWithItsVerdict() {
        final @NotNull List<TestRunItems> asTheFolderListedThem = List.of(
                result(DELETED_TEST_CASE, TestStatus.FAILED),
                result(SECOND, TestStatus.PASSED),
                result(FIRST, TestStatus.PASSED));

        final @NotNull List<TestRunItems> ordered = IndexingScanner.inTestCaseOrder(asTheFolderListedThem, aProjectHoldingBothTestCases());

        assertEquals(idsOf(ordered), List.of(FIRST, SECOND, DELETED_TEST_CASE),
                "A run outlives the cases it was made from, and a result whose case is gone has no place in their"
                        + " order - so it comes after them rather than being dropped or sorted among them");

        assertEquals(ordered.getLast().getStatus(), TestStatus.FAILED,
                "What the run recorded about a case that has since been deleted is still its record: deleting the"
                        + " case must not change what the run found");
    }

}
