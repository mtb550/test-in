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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CopyChoiceTest {

    @Test
    public void noTwoRowsAnswerToTheSameKey() {
        final @NotNull Map<KeyStroke, List<CopyChoice>> byKey = new HashMap<>();

        for (final CopyChoice choice : CopyChoice.values()) {
            byKey.computeIfAbsent(choice.getShortcut().getKey(), key -> new ArrayList<>()).add(choice);
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
}
