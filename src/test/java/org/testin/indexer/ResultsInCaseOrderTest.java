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

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

/**
 * Rule-INTERNAL-011, Rule-INTERNAL-012.
 * <p>
 * A run's results come back in their cases' test-set order (#305, S19).
 * <p>
 * The results are one file per case now, so the only order the scan gets for
 * free is the one the file system happens to list the folder in - which is a
 * different order on another machine and no order at all to a tester. The run
 * editor draws them in it, every report prints them in it and every export
 * writes them in it, so the order is decided once, where the results are read,
 * and it is the order the cases sit in their test sets.
 * <p>
 * A result whose test case the project no longer holds cannot take a place in
 * that order, and it is still a record of work: it comes last, with the verdict
 * it was given.
 * <p>
 * A unit test because the ordering is pure - a list of results and the cases one
 * pass read, in, an ordered list out - and reached by reflection because it is
 * {@code IndexingScanner}'s own, private to the one reader that needs it. The
 * same reason {@code NodeKindTablesTest} reaches for a private table: nothing
 * but a test has any use for it, and it is worth no production API of its own.
 */
public class ResultsInCaseOrderTest {

    /** Ranked "a": first in its test set, whatever a folder listing says. */
    private static final @NotNull UUID FIRST = UUID.fromString("11111111-1111-4111-8111-111111111101");

    /** Ranked "b", so it follows {@link #FIRST}. */
    private static final @NotNull UUID SECOND = UUID.fromString("11111111-1111-4111-8111-111111111102");

    /** A case the run recorded a verdict for and the project does not hold any more. */
    private static final @NotNull UUID DELETED_CASE = UUID.fromString("11111111-1111-4111-8111-111111111103");

    /**
     * The two cases a scan found, ranked, as the index holds them.
     */
    private static @NotNull ScannedProject aProjectHoldingBothCases() {
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
    public void theResultsComeBackInTheirCasesTestSetOrder() {
        final @NotNull List<TestRunItems> asTheFolderListedThem =
                List.of(result(SECOND, TestStatus.FAILED), result(FIRST, TestStatus.PASSED));

        final @NotNull List<TestRunItems> ordered = inCaseOrder(asTheFolderListedThem, aProjectHoldingBothCases());

        assertEquals(idsOf(ordered), List.of(FIRST, SECOND),
                "A run's results are drawn, printed and exported in this order, so it is the order the cases sit"
                        + " in their test sets - never the order the folder happened to list their files in");
    }

    @Test
    public void aResultWhoseCaseIsGoneComesLastWithItsVerdict() {
        final @NotNull List<TestRunItems> asTheFolderListedThem = List.of(
                result(DELETED_CASE, TestStatus.FAILED),
                result(SECOND, TestStatus.PASSED),
                result(FIRST, TestStatus.PASSED));

        final @NotNull List<TestRunItems> ordered = inCaseOrder(asTheFolderListedThem, aProjectHoldingBothCases());

        assertEquals(idsOf(ordered), List.of(FIRST, SECOND, DELETED_CASE),
                "A run outlives the cases it was made from, and a result whose case is gone has no place in their"
                        + " order - so it comes after them rather than being dropped or sorted among them");

        assertEquals(ordered.getLast().getStatus(), TestStatus.FAILED,
                "What the run recorded about a case that has since been deleted is still its record: deleting the"
                        + " case must not change what the run found");
    }

    /**
     * {@code IndexingScanner.inCaseOrder}, which is private to the reader that
     * owns it.
     */
    @SuppressWarnings("unchecked")
    private static @NotNull List<TestRunItems> inCaseOrder(final @NotNull List<TestRunItems> results, final @NotNull ScannedProject scanned) {
        try {
            final @NotNull Method ordering = IndexingScanner.class.getDeclaredMethod("inCaseOrder", List.class, ScannedProject.class);
            ordering.setAccessible(true);

            return (List<TestRunItems>) ordering.invoke(null, results, scanned);
        } catch (final ReflectiveOperationException ex) {
            throw new AssertionError("IndexingScanner.inCaseOrder is gone or takes something else now, so nothing"
                    + " checks the order every run editor, report and export prints: " + ex.getMessage(), ex);
        }
    }
}
