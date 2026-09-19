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

package org.testin.testrun;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestStatus;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import java.util.Arrays;
import java.util.List;

/**
 * Every verdict a test case can be given, as one thing the platform owns (#119).
 * <p>
 * A group rather than an action per status, because a verdict is a constant on
 * {@link TestStatus} and a new one is meant to appear here without anybody
 * remembering to add it (#37). Writing an {@code <action>} element each would
 * put the list in {@code plugin.xml} as well as in the enum, and the enum is
 * where a verdict is declared.
 * <p>
 * Compact rather than a submenu, so Passed, Failed and Blocked sit at the top of
 * the run editor's menu where they always have.
 */
public class SetTestCaseStatusGroup extends DefaultActionGroup {

    /**
     * UC-EDITOR-PANEL-032, Rule-EDITOR-PANEL-133.
     * <p>
     * The verdicts are added once, when the platform makes the group, rather
     * than built each time it is asked. The grid's key binder reads a group's
     * entries through the public {@code getChildren(ActionManager)}, which
     * returns what was added; the form that asks the group is the platform's
     * alone to call, and calling it failed the Marketplace's verifier (#324).
     */
    public SetTestCaseStatusGroup() {
        super(verdicts());
    }

    /**
     * UC-EDITOR-PANEL-032.
     * <p>
     * Puts each verdict's letter on the run editor's list.
     * <p>
     * Not in the keymap, and this is the reason: P, F and B carry no modifier,
     * so a keymap entry would answer them anywhere in the IDE. On the list they
     * are that list's gesture, which is what they have always been.
     * <p>
     * The actions are built and discarded, the way every key registered on a
     * component in this plugin is - registering is what they were made for.
     */
    public static void bindLettersTo(final @NotNull JComponent list) {
        verdicts().forEach(action -> action.registerCustomShortcutSet(action.getShortcutSet(), list));
    }

    /**
     * UC-EDITOR-PANEL-032, Rule-EDITOR-PANEL-213.
     * <p>
     * One action per user-settable status, in the order the enum declares them,
     * each carrying its own letter.
     * <p>
     * Carried rather than attached by whoever binds it. Only the card list used
     * to attach the letters, so the grid's binder - which puts each menu entry's
     * own key on the table - found three actions with no key and bound nothing:
     * in grid view P, F and B did nothing, and a verdict could be given with the
     * mouse but not the keyboard (#66, finding 201).
     */
    private static @NotNull List<SetTestCaseStatusAction> verdicts() {
        return Arrays.stream(TestStatus.values())
                .filter(TestStatus::isVerdict)
                .map(status -> {
                    final @NotNull SetTestCaseStatusAction action = new SetTestCaseStatusAction(status);

                    // No component: this only gives the action its letter, which
                    // the menu shows and each binder registers where it belongs.
                    // setShortcutSet does the same and is internal to the
                    // platform, so the Marketplace's verifier refuses it (#324).
                    action.registerCustomShortcutSet(Shortcuts.customShortcut(status.getMenuEntry().shortcut()), null);
                    return action;
                })
                .toList();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
