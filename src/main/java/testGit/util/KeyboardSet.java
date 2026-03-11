package testGit.util;

import com.intellij.openapi.actionSystem.CustomShortcutSet;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public enum KeyboardSet {
    DeletePackage(KeyEvent.VK_DELETE, 0),
    Rename(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK),
    CreateTestCase(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK),
    Escape(KeyEvent.VK_ESCAPE, 0),
    Enter(KeyEvent.VK_ENTER, 0),
    ContextMenu(KeyEvent.VK_CONTEXT_MENU, 0),
    UpdateTestCase(KeyEvent.VK_F2, 0),
    RunTestCase(KeyEvent.VK_F5, 0),
    NavigateToCode(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK),
    GenerateTestCase(KeyEvent.VK_F12, InputEvent.CTRL_DOWN_MASK);

    private final int keyCode;
    private final int modifiers;

    KeyboardSet(int keyCode, int modifiers) {
        this.keyCode = keyCode;
        this.modifiers = modifiers;
    }

    @SuppressWarnings("MagicConstant")
    public CustomShortcutSet get() {
        return new CustomShortcutSet(KeyStroke.getKeyStroke(this.keyCode, this.modifiers));
    }
}