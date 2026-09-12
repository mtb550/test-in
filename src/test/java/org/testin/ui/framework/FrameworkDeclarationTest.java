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

package org.testin.ui.framework;

import org.testin.util.Shortcuts;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

/**
 * The framework's declaration contracts: a forgotten declaration part fails
 * with a message naming it, status bar entries keep the shown-equals-bound
 * invariant, and a selection component cannot be declared empty.
 */
public class FrameworkDeclarationTest {

    @Test
    public void forgottenDeclarationPartIsNamedInTheFailure() {
        final NullPointerException missingTitle = expectThrows(NullPointerException.class, () ->
                DialogDto.builder().components(List.of()).shortcuts(List.of()).build());
        assertTrue(missingTitle.getMessage().contains("title"), missingTitle.getMessage());

        final NullPointerException missingShortcuts = expectThrows(NullPointerException.class, () ->
                DialogDto.builder().title("t").components(List.of()).build());
        assertTrue(missingShortcuts.getMessage().contains("shortcuts"), missingShortcuts.getMessage());

        final NullPointerException missingComponents = expectThrows(NullPointerException.class, () ->
                DialogDto.builder().title("t").shortcuts(List.of()).build());
        assertTrue(missingComponents.getMessage().contains("components"), missingComponents.getMessage());
    }

    @Test
    public void hintEntriesRenderButNeverBind() {
        final StatusBarShortcut hint = StatusBarShortcut.hint("↑ ↓", "Select");

        assertFalse(hint.isBindable(), "a hint must never be bound to a key");
        assertEquals(hint.getShortcutText(), "↑ ↓");
        assertEquals(hint.getName(), "Select");
    }

    @Test
    public void builtEntriesCarryKeyNameAndAction() {
        final Runnable action = () -> {
        };
        final StatusBarShortcut entry = new StatusBarShortcut(
                org.testin.util.Shortcuts.Enter, "Enter", "Confirm", action);

        assertTrue(entry.isBindable(), "a built entry must bind");
        assertEquals(entry.getName(), "Confirm");
        assertEquals(entry.getShortcutText(), "Enter");
        assertEquals(entry.action(), action);
    }

    @Test
    public void selectionComponentCannotBeDeclaredEmpty() {
        final IllegalStateException error = expectThrows(IllegalStateException.class, () ->
                ComponentDialogBase.textFieldWithSelections().build());
        assertTrue(error.getMessage().contains("selection"), error.getMessage());
    }
}
