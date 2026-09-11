package org.testin.testcase;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.statusbar.StatusBarItem;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A key the test case dialogs advertise in their status bar.
 * <p>
 * Only a name and the keystrokes it is spelled with — these are hints, not
 * fields. The sections bind the keys themselves; this says what to print. Kept
 * apart from {@link CreateTestCaseFields} because the two are different things:
 * a field has an icon, a dialog section and a placeholder, and a key has none
 * of those. Holding both in one enum meant five of its eight constructor
 * arguments were null for two thirds of its constants.
 */
@Getter
public enum TestCaseDialogKey implements StatusBarItem {

    SAVE(StatusBarShortcut.SAVE, Shortcuts.Enter),
    CANCEL(Bundle.message("dialog.key.cancel"), Shortcuts.Escape),

    // Bound by the platform on the spell-checked editors, not by us; shown only
    // on the sections that actually check spelling, so the hint is never a lie.
    CORRECTIONS(Bundle.message("dialog.key.corrections"), Shortcuts.Corrections),

    ADD_STEP(Bundle.message("dialog.key.add.step"), Shortcuts.CreateTestCaseAddStep),
    AUTO_COMPLETE(Bundle.message("dialog.key.auto.complete"), Shortcuts.AutoComplete),
    ADD_GROUP(Bundle.message("dialog.key.add.group"), Shortcuts.CreateTestCaseGroup),

    NAVIGATE_TAB(Bundle.message("dialog.key.navigate"), Shortcuts.TabNext, Shortcuts.TabPrevious),
    NAVIGATE_ARROWS(Bundle.message("dialog.key.navigate.priority"), Shortcuts.ArrowUp, Shortcuts.ArrowDown);

    private final @NotNull String name;

    /**
     * One or more keystrokes. Several because a hint often names alternatives —
     * Tab or Shift+Tab, the three priority keys — which used to be spelled out
     * as a pre-joined string per constant.
     */
    private final Shortcuts @NotNull [] keys;

    TestCaseDialogKey(final @NotNull String name, final Shortcuts @NotNull ... keys) {
        this.name = name;
        this.keys = keys;
    }

    @Override
    public @NotNull String getShortcutText() {
        return Arrays.stream(keys)
                .map(Shortcuts::getShortcutText)
                .collect(Collectors.joining(" / "));
    }
}
