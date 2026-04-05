package testGit.util;

import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.keymap.KeymapUtil;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public enum KeyboardSet {
    DeletePackage(KeyEvent.VK_DELETE, 0),
    RenameNode(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK),
    CreateTestCase(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK),
    Escape(KeyEvent.VK_ESCAPE, 0),
    Enter(KeyEvent.VK_ENTER, 0),
    OpenContextMenu(KeyEvent.VK_CONTEXT_MENU, 0),
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
    CopyTestCaseTitle(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK),
    NextTestCase(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK),
    PreviousTestCase(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK),
    SaveAlternate(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK),
    AddStep(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
    TabNext(KeyEvent.VK_TAB, 0),
    TabPrevious(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK),
    ArrowDown(KeyEvent.VK_DOWN, 0),
    ArrowUp(KeyEvent.VK_UP, 0),
    CreateTestCaseTitle(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK),
    CreateTestCaseExpected(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK),
    CreateTestCaseAddStep(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
    CreateTestCaseRemoveStep(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK),
    CreateTestCaseGroups(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK),
    CreateTestCasePriority(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK),
    UpdateTestCaseTitle(KeyEvent.VK_T, 0),
    UpdateTestCaseExpected(KeyEvent.VK_E, 0),
    UpdateTestCaseSteps(KeyEvent.VK_S, 0),
    UpdateTestCasePriority(KeyEvent.VK_P, 0),
    UpdateTestCaseGroups(KeyEvent.VK_G, 0);

    private final int keyCode;
    private final int modifiers;

    KeyboardSet(final int keyCode, final int modifiers) {
        this.keyCode = keyCode;
        this.modifiers = modifiers;
    }

    @SuppressWarnings("MagicConstant")
    public static CustomShortcutSet getShortcutFor(int keyCode, int modifiers) {
        return new CustomShortcutSet(KeyStroke.getKeyStroke(keyCode, modifiers));
    }

    @SuppressWarnings("MagicConstant")
    public CustomShortcutSet getShortcut() {
        return new CustomShortcutSet(KeyStroke.getKeyStroke(this.keyCode, this.modifiers));
    }

    @SuppressWarnings("MagicConstant")
    public String getShortcutText() {
        return KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(this.keyCode, this.modifiers));
    }
}