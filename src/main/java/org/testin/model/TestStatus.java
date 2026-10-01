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

import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.FixedColors;

import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.event.KeyEvent;

@Getter
@AllArgsConstructor
public enum TestStatus {
    PASSED(
            JBColor.GREEN,
            FixedColors.GREEN.darker(),
            Bundle.message("status.verdict.passed"),
            new MenuEntry(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0)),
            false
    ),

    FAILED(
            JBColor.RED.darker(),
            FixedColors.RED.darker(),
            Bundle.message("status.verdict.failed"),
            new MenuEntry(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0)),
            true
    ),

    BLOCKED(
            JBColor.ORANGE,
            FixedColors.ORANGE.darker(),
            Bundle.message("status.verdict.blocked"),
            new MenuEntry(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0)),
            false
    ),

    PENDING(
            JBColor.lazy(UIUtil::getContextHelpForeground),
            FixedColors.GRAY.darker(),
            Bundle.message("status.verdict.pending"),
            MenuEntry.NONE,
            false
    ),

    REMOVED(
            JBColor.GRAY,
            FixedColors.GRAY.darker(),
            Bundle.message("status.verdict.removed"),
            MenuEntry.NONE,
            false
    ),

    UNTESTED(
            JBColor.GRAY.brighter(),
            FixedColors.GRAY.darker(),
            Bundle.message("status.verdict.untested"),
            MenuEntry.NONE,
            false
    );

    private final @NotNull Color rowColor;
    private final @NotNull Color reportColor;
    private final @NotNull String label;

    private final @NotNull MenuEntry menuEntry;

    private final boolean collectsFailureDetails;

    // Rule-REPORT-025
    public @NotNull String getReportHex() {
        return ColorUtil.toHex(reportColor);
    }

    // Rule-REPORT-025
    public @NotNull String getReportDarkHex() {
        return ColorUtil.toHex(ReportColor.forDarkPage(reportColor));
    }

    public boolean isVerdict() {
        return menuEntry != MenuEntry.NONE;
    }
}
