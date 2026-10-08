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

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class UpdateMenuLettersTest {

    // Rule-EDITOR-PANEL-194
    @Test
    public void statusSitsLastWithNoLetterAndNoIcon() {
        final @NotNull List<UpdateTestCaseFields> menu = List.of(UpdateTestCaseFields.values());

        assertEquals(menu.getLast(), UpdateTestCaseFields.STATUS, "the one without a letter sits last");
        assertTrue(Shortcuts.isNoKey(UpdateTestCaseFields.STATUS.getShortcut().getKey()), "Status has a letter");
        assertSame(UpdateTestCaseFields.STATUS.getIcon(), EmptyIcon.ICON_16, "Status has an icon");
        for (final UpdateTestCaseFields field : menu.subList(0, menu.size() - 1)) {
            assertFalse(Shortcuts.isNoKey(field.getShortcut().getKey()), field + " sits above the one without a letter, so it needs one");
        }
    }

    // Rule-EDITOR-PANEL-194
    @Test
    public void referenceIsOpenedByRAndDrawsR() {
        assertEquals(UpdateTestCaseFields.REFERENCE.getShortcut().getKey(), KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "Reference is not opened by R");
        assertEquals(UpdateTestCaseFields.iconOf(TestSetEditorAttributes.REFERENCE.getName()), Optional.of(CopyChoice.REFERENCE.getIcon()), "Reference does not draw the R the copy menu draws");
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
