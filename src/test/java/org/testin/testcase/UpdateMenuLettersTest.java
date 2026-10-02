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

import com.intellij.util.ui.EmptyIcon;
import org.jetbrains.annotations.NotNull;
import org.testin.clipboard.CopyChoice;
import org.testin.util.Shortcuts;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class UpdateMenuLettersTest {

    // Rule-EDITOR-PANEL-194
    @Test
    public void statusAndReferenceSitLastWithNoLetterAndNoIcon() {
        final @NotNull List<UpdateTestCaseFields> menu = List.of(UpdateTestCaseFields.values());

        assertEquals(menu.subList(menu.size() - 2, menu.size()), List.of(UpdateTestCaseFields.STATUS, UpdateTestCaseFields.REFERENCE), "the two without a letter sit last");
        for (final UpdateTestCaseFields field : List.of(UpdateTestCaseFields.STATUS, UpdateTestCaseFields.REFERENCE)) {
            assertTrue(Shortcuts.isNoKey(field.getShortcut().getKey()), field + " has a letter");
            assertSame(field.getIcon(), EmptyIcon.ICON_16, field + " has an icon");
        }
        for (final UpdateTestCaseFields field : menu.subList(0, menu.size() - 2)) {
            assertFalse(Shortcuts.isNoKey(field.getShortcut().getKey()), field + " sits above the two without a letter, so it needs one");
        }
    }

    // Rule-EDITOR-PANEL-209
    @Test
    public void theCopyMenuUsesTheUpdateMenusLetterForTheSameField() {
        for (final UpdateTestCaseFields field : UpdateTestCaseFields.values()) {
            if (Shortcuts.isNoKey(field.getShortcut().getKey())) continue;

            Arrays.stream(CopyChoice.values())
                    .filter(choice -> choice.getName().equals(field.getName()))
                    .forEach(choice -> assertEquals(choice.getShortcut().getKey(), field.getShortcut().getKey(), field.getName() + " is a different letter on the copy menu than on the update menu"));
        }
    }
}
