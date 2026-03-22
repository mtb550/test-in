package testGit.util;

import com.intellij.openapi.actionSystem.CustomShortcutSet;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public enum KeyboardSet {
    DeletePackage(KeyEvent.VK_DELETE, 0),
    RenameNode(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK),
    CreateTestCase(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK),
    Escape(KeyEvent.VK_ESCAPE, 0),
    Enter(KeyEvent.VK_ENTER, 0),
    ContextMenu(KeyEvent.VK_CONTEXT_MENU, 0),
    UpdateTestCase(KeyEvent.VK_F2, 0),
    RunTestCase(KeyEvent.VK_F5, 0),
    NavigateToCode(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK),
    GenerateTestCase(KeyEvent.VK_F12, InputEvent.CTRL_DOWN_MASK),
    Undo(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK),
    Redo(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK),
    CreateNode(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK),
    CopyNode(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK),
    CutNode(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK),
    PasteNode(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK),
    CopyTestCaseTitle(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);

    private final int keyCode;
    private final int modifiers;

    KeyboardSet(final int keyCode, final int modifiers) {
        this.keyCode = keyCode;
        this.modifiers = modifiers;
    }

    @SuppressWarnings("MagicConstant")
    public CustomShortcutSet getShortcut() {
        return new CustomShortcutSet(KeyStroke.getKeyStroke(this.keyCode, this.modifiers));
    }
}