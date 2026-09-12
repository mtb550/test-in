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
import org.testng.annotations.Test;

import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * The copy menu's rows, and the one thing about them nothing else can check.
 * <p>
 * Every row is a letter the tester types while the menu is open. Two rows on
 * one letter is the failure this project has a rule against - whichever the
 * menu consults first silently answers for both - and it is invisible in
 * review, because the two constants sit twelve lines apart and name different
 * fields (#108).
 */
public class CopyChoiceTest {

    /**
     * UC-EDITOR-PANEL-036, Rule-EDITOR-PANEL-158.
     */
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

    /**
     * Every row but the first copies one attribute, and takes its caption from
     * it - so a field renamed in {@code TestEditorAttributes} renames the row
     * with it, rather than leaving this menu saying what it used to be called.
     */
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

    /**
     * Every row prints its key, because a menu row that shows no shortcut is one
     * a tester can only reach with the mouse.
     */
    @Test
    public void everyRowPrintsItsKey() {
        assertTrue(Arrays.stream(CopyChoice.values()).noneMatch(choice -> choice.getShortcutText().isBlank()),
                "a copy row has no key to print, so the menu shows a blank where its shortcut goes");
    }
}
