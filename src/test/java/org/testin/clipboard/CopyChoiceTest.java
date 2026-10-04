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

package org.testin.clipboard;

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testng.annotations.Test;

import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testin.model.Priority;
import org.testin.model.TestCaseStatus;
import org.testin.testcase.TestCaseEditorAttributes;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class CopyChoiceTest {

    @Test
    public void noTwoRowsAnswerToTheSameKey() {
        final @NotNull Map<KeyStroke, List<CopyChoice>> byKey = new HashMap<>();

        for (final CopyChoice choice : CopyChoice.values()) {
            byKey.computeIfAbsent(choice.getShortcut().getKey(), _ -> new ArrayList<>()).add(choice);
        }

        final @NotNull List<String> shared = byKey.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey() + " -> " + entry.getValue())
                .toList();

        assertTrue(shared.isEmpty(),
                "two rows of the copy menu answer to one key, so one of them can never be chosen: " + shared);
    }

    @Test
    public void everyRowButAllDetailsIsNamedByTheAttributeItCopies() {
        for (final CopyChoice choice : CopyChoice.values()) {
            if (choice == CopyChoice.ALL_DETAILS) {
                assertTrue(choice.getAttribute().isEmpty(), "ALL_DETAILS copies everything, so it names no single attribute");
                continue;
            }

            assertTrue(choice.getAttribute().isPresent(), choice + " copies no attribute, so there is nothing for it to copy");
            assertEquals(choice.getName(), choice.getAttribute().orElseThrow().getName(),
                    choice + " is captioned something other than the attribute it copies, so the two can drift apart");
        }
    }

    @Test
    public void everyRowPrintsItsKey() {
        assertTrue(Arrays.stream(CopyChoice.values()).noneMatch(choice -> choice.getShortcutText().isBlank()),
                "a copy row has no key to print, so the menu shows a blank where its shortcut goes");
    }

    @Test
    public void theClassNameIsCopiedDotted() {
        final @NotNull TestSetDirectoryDto set = new TestSetDirectoryDto();
        set.setPath2(new ArrayList<>(List.of("shop", "checkout", "Payment")));

        final @NotNull TestCaseDto tc = TestCaseDto.builder().description("card is declined").build();
        tc.setParent(set);

        assertEquals(CopyChoice.FQCN.from(tc), "shop.checkout.PaymentTest.cardIsDeclined");
    }

    private static @NotNull TestCaseDto writtenInFull() {
        final @NotNull TestSetDirectoryDto set = new TestSetDirectoryDto();
        set.setPath2(new ArrayList<>(List.of("shop", "checkout", "Payment")));

        final @NotNull TestCaseDto tc = TestCaseDto.builder()
                .id(UUID.randomUUID())
                .description("Refuse an expired card")
                .expectedResult("The payment is declined")
                .steps(List.of("Enter the card", "Press Pay"))
                .priority(Priority.HIGH)
                .reference("JIRA-77")
                .testData("card=4111")
                .preConditions("A basket holds one item")
                .group(List.of("Regression"))
                .module("payments")
                .status(TestCaseStatus.REVIEWED)
                .build();
        tc.setParent(set);
        return tc;
    }

    // Rule-EDITOR-PANEL-073, Rule-EDITOR-PANEL-207
    @Test
    public void allDetailsCopiesEveryFieldTheTesterWroteAsNameColonValue() {
        final @NotNull TestCaseDto tc = writtenInFull();
        final @NotNull List<TestCaseEditorAttributes> written = List.of(
                TestCaseEditorAttributes.DESCRIPTION, TestCaseEditorAttributes.EXPECTED_RESULT, TestCaseEditorAttributes.STEPS,
                TestCaseEditorAttributes.PRIORITY, TestCaseEditorAttributes.REFERENCE, TestCaseEditorAttributes.TEST_DATA,
                TestCaseEditorAttributes.PRE_CONDITIONS, TestCaseEditorAttributes.GROUP, TestCaseEditorAttributes.MODULE,
                TestCaseEditorAttributes.STATUS);

        final @NotNull String copied = CopyChoice.ALL_DETAILS.from(tc);

        for (final TestCaseEditorAttributes field : written) {
            assertTrue(copied.contains(field.getName() + ": " + field.gridValue(tc)), field.getName() + " is not copied as its name, a colon and its value: " + copied);
        }
        for (final TestCaseEditorAttributes field : TestCaseEditorAttributes.values()) {
            if (written.contains(field)) continue;

            assertFalse(copied.contains(field.getName() + ": "), field.getName() + " is not a field the tester wrote, yet All Details copied it: " + copied);
        }
    }

    // Rule-EDITOR-PANEL-207
    @Test
    public void aFieldLeftEmptyIsNotALine() {
        final @NotNull TestCaseDto tc = writtenInFull();
        tc.setReference("");

        assertFalse(CopyChoice.ALL_DETAILS.from(tc).contains(TestCaseEditorAttributes.REFERENCE.getName() + ":"), "an empty reference still got a line");
    }

    // Rule-EDITOR-PANEL-074
    @Test
    public void severalTestCasesAreSeparatedByABlankLine() {
        final @NotNull TestCaseDto first = TestCaseDto.builder().description("Log in").build();
        final @NotNull TestCaseDto second = TestCaseDto.builder().description("Log out").build();

        assertEquals(CopyChoice.DESCRIPTION.from(List.of(first, second)), """
                Log in

                Log out""");
    }

    // Rule-EDITOR-PANEL-208
    @Test
    public void aSingleValueIsCopiedBareAndSoAreTheValuesTheTesterNeverWrites() {
        final @NotNull TestCaseDto tc = writtenInFull();

        assertEquals(CopyChoice.DESCRIPTION.from(tc), "Refuse an expired card", "a single value carries no caption");
        assertEquals(CopyChoice.ID.from(tc), tc.getId().toString(), "the identity can be copied on its own");
        assertEquals(CopyChoice.FQCN.from(tc), "shop.checkout.PaymentTest.refuseAnExpiredCard", "and the class name");
        assertEquals(CopyChoice.PATH.from(tc), TestCaseEditorAttributes.PATH.gridValue(tc), "and the path");
    }
}
