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

package org.testin.model;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.event.KeyEvent;

@Getter
@AllArgsConstructor
public enum TestStatus {
    PASSED(
            "008000",
            JBColor.GREEN,
            Bundle.message("status.verdict.passed"),
            new MenuEntry(AllIcons.Actions.Checked, KeyStroke.getKeyStroke(KeyEvent.VK_P, 0)),
            false
    ),

    FAILED(
            "FF0000",
            JBColor.RED.darker(),
            Bundle.message("status.verdict.failed"),
            new MenuEntry(AllIcons.Actions.Cancel, KeyStroke.getKeyStroke(KeyEvent.VK_F, 0)),
            true
    ),

    BLOCKED(
            "FFA500",
            JBColor.ORANGE,
            Bundle.message("status.verdict.blocked"),
            new MenuEntry(AllIcons.Actions.Pause, KeyStroke.getKeyStroke(KeyEvent.VK_B, 0)),
            false
    ),

    PENDING(
            "808080",
            JBColor.lazy(UIUtil::getContextHelpForeground),
            Bundle.message("status.verdict.pending"),
            MenuEntry.NONE,
            false
    ),

    REMOVED(
            "9E9E9E",
            JBColor.GRAY,
            Bundle.message("status.verdict.removed"),
            MenuEntry.NONE,
            false
    ),

    UNTESTED(
            "808080",
            JBColor.GRAY.brighter(),
            Bundle.message("status.verdict.untested"),
            MenuEntry.NONE,
            false
    );

    private final @NotNull String hex;
    private final @NotNull Color rowColor;
    private final @NotNull String label;

    private final @NotNull MenuEntry menuEntry;

    private final boolean collectsFailureDetails;

    public boolean isVerdict() {
        return menuEntry != MenuEntry.NONE;
    }

    public record MenuEntry(@NotNull Icon icon, @NotNull KeyStroke shortcut) {
        public static final @NotNull MenuEntry NONE =
                new MenuEntry(EmptyIcon.ICON_16, KeyStroke.getKeyStroke(KeyEvent.VK_UNDEFINED, 0));
    }
}
