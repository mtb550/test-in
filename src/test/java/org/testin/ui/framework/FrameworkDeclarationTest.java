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
