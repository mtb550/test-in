package org.testin.enums;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.statusBar.StatusBarItem;
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

    SAVE("Save", Shortcuts.Enter),
    CANCEL("Cancel", Shortcuts.Escape),

    // Bound by the platform on the spell-checked editors, not by us; shown only
    // on the sections that actually check spelling, so the hint is never a lie.
    CORRECTIONS("Corrections", Shortcuts.Corrections),

    ADD_STEP("Add Step", Shortcuts.CreateTestCaseAddStep),
    REMOVE_STEP("Remove Step", Shortcuts.CreateTestCaseRemoveStep),
    AUTO_COMPLETE("Auto Complete", Shortcuts.AutoComplete),
    SELECT_GROUP("Select / Unselect Group", Shortcuts.SelectGroup),

    NAVIGATE_TAB("Navigate", Shortcuts.TabNext, Shortcuts.TabPrevious),
    NAVIGATE_ARROWS("Navigate Priority", Shortcuts.ArrowUp, Shortcuts.ArrowDown),
    SET_PRIORITY("Set Priority", Shortcuts.PriorityHigh, Shortcuts.PriorityMedium, Shortcuts.PriorityLow);

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
