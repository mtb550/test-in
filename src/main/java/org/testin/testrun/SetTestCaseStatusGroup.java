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
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    // UC-EDITOR-PANEL-032, Rule-EDITOR-PANEL-133
    @Override
    public AnAction @NotNull [] getChildren(final @Nullable AnActionEvent e) {
        return verdicts().toArray(AnAction[]::new);
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
        verdicts().forEach(action -> action.registerCustomShortcutSet(
                Shortcuts.customShortcut(action.getStatus().getMenuEntry().shortcut()), list));
    }

    /**
     * One action per user-settable status, in the order the enum declares them.
     */
    private static @NotNull List<SetTestCaseStatusAction> verdicts() {
        return Arrays.stream(TestStatus.values())
                .filter(TestStatus::isVerdict)
                .map(SetTestCaseStatusAction::new)
                .toList();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
