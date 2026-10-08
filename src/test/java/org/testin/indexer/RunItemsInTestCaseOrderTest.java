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
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class RunItemsInTestCaseOrderTest {

    private static final @NotNull UUID FIRST = UUID.fromString("11111111-1111-4111-8111-111111111101");

    private static final @NotNull UUID SECOND = UUID.fromString("11111111-1111-4111-8111-111111111102");

    private static final @NotNull UUID DELETED_TEST_CASE = UUID.fromString("11111111-1111-4111-8111-111111111103");

    private static @NotNull ScannedProject aProjectHoldingBothTestCases() {
        final @NotNull ScannedProject scanned = new ScannedProject();
        scanned.getTestCasesById().put(FIRST, TestCaseDto.builder().id(FIRST).order("a").build());
        scanned.getTestCasesById().put(SECOND, TestCaseDto.builder().id(SECOND).order("b").build());

        return scanned;
    }

    private static @NotNull RunItem runItem(final @NotNull UUID testCaseId, final @NotNull RunItemStatus status) {
        return RunItem.builder().id(testCaseId).status(status).build();
    }

    private static @NotNull List<UUID> idsOf(final @NotNull List<RunItem> runItems) {
        return runItems.stream().map(RunItem::getId).toList();
    }

    @Test
    public void theRunItemsComeBackInTheirTestCasesTestSetOrder() {
        final @NotNull List<RunItem> asTheFolderListedThem =
                List.of(runItem(SECOND, RunItemStatus.FAILED), runItem(FIRST, RunItemStatus.PASSED));

        final @NotNull List<RunItem> ordered = IndexingScanner.inTestCaseOrder(asTheFolderListedThem, aProjectHoldingBothTestCases());

        assertEquals(idsOf(ordered), List.of(FIRST, SECOND),
                "A test run's results are drawn, printed and exported in this order, so it is the order the test cases sit"
                        + " in their test sets - never the order the folder happened to list their files in");
    }

    @Test
    public void aRunItemWhoseTestCaseIsGoneComesLastWithItsRunItemStatus() {
        final @NotNull List<RunItem> asTheFolderListedThem = List.of(
                runItem(DELETED_TEST_CASE, RunItemStatus.FAILED),
                runItem(SECOND, RunItemStatus.PASSED),
                runItem(FIRST, RunItemStatus.PASSED));

        final @NotNull List<RunItem> ordered = IndexingScanner.inTestCaseOrder(asTheFolderListedThem, aProjectHoldingBothTestCases());

        assertEquals(idsOf(ordered), List.of(FIRST, SECOND, DELETED_TEST_CASE),
                "A test run outlives the test cases it was made from, and a run item whose test case is gone has no place in their"
                        + " order - so it comes after them rather than being dropped or sorted among them");

        assertEquals(ordered.getLast().getStatus(), RunItemStatus.FAILED,
                "What the test run recorded about a test case that has since been deleted is still its record: deleting the"
                        + " test case must not change what the test run found");
    }

}
