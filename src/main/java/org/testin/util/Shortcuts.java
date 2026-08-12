package org.testin.util;

import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.Shortcut;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.swing.*;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Keystrokes shared by more than one class. Single-use keystrokes live as
 * constants in their owning action/enum class; the helper methods are in
 * {@link Tools}.
 */
@Getter
@AllArgsConstructor
public enum Shortcuts {

    // Dialog confirm / dismiss
    Enter(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)),
    Escape(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)),

    // Toolbar search (test editor + run editor)
    FocusSearch(KeyStroke.getKeyStroke(KeyEvent.VK_F, menuMask())),

    // Item operations shared between the project tree, editors, and details panel
    CreateItem(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK)),
    UpdateItem(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0)),
    CopyItem(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK)),
    DeletePackage(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)),

    // Card actions (context menu + hover icons)
    RunTestCase(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)),
    NavigateToCode(KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK)),

    // Page navigation (status bar tooltips + page actions)
    NextTestCase(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK)),
    PreviousTestCase(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK)),

    // Field navigation inside the create/update dialogs
    TabNext(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)),
    TabPrevious(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)),
    ArrowDown(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
    ArrowUp(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
    AutoComplete(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK)),
    SelectGroup(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)),

    // Test case fields (create dialog sections + fields enums)
    CreateTestCaseDescription(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK)),
    CreateTestCaseExpectedResult(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK)),
    CreateTestCaseModule(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK)),
    CreateTestCaseTestData(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK)),
    CreateTestCasePreConditions(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK)),
    CreateTestCaseAddStep(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)),
    CreateTestCaseRemoveStep(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK)),
    CreateTestCaseGroup(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK)),
    CreateTestCasePriority(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK)),

    // Test case update-menu fields (update dialogs + fields enums)
    UpdateTestCaseDescription(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0)),
    UpdateTestCaseExpectedResult(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0)),
    UpdateTestCaseModule(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0)),
    UpdateTestCaseTestData(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0)),
    UpdateTestCasePreConditions(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0)),
    UpdateTestCaseSteps(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0)),
    UpdateTestCasePriority(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0)),
    UpdateTestCaseGroup(KeyStroke.getKeyStroke(KeyEvent.VK_G, 0)),

    // Priority selection (bug priority, test case priority, fields enums)
    PriorityEmpty(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0)),
    PriorityHigh(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0)),
    PriorityMedium(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0)),
    PriorityLow(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0));

    private final KeyStroke key;

    /**
     * The platform menu modifier (Cmd on macOS, Ctrl elsewhere), same source
     * as the other cross-platform shortcuts; plain Ctrl in headless test runs.
     */
    private static int menuMask() {
        try {
            return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        } catch (final HeadlessException ex) {
            return InputEvent.CTRL_DOWN_MASK;
        }
    }

    public CustomShortcutSet getCustomShortcut() {
        return Tools.customShortcut(key);
    }

    public Shortcut getShortcut() {
        return Tools.keyboardShortcut(key);
    }

    public String getShortcutText() {
        return Tools.shortcutText(key);
    }

    public boolean matches(final KeyEvent e) {
        return Tools.matches(e, key);
    }
}
