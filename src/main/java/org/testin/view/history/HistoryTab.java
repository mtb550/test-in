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

package org.testin.view.history;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import org.testin.util.Bundle;
import javax.swing.*;
import java.awt.*;

public class HistoryTab {

    /// UC-VIEW-PANEL-007, Rule-VIEW-PANEL-037.
    ///
    /// An honest empty state, because a test case records one edit and forgets
    /// the rest - there is no history to show. Recording it is #150. This drew
    /// demo data once, which read as a working feature holding somebody else's
    /// data.
    public void load(final @NotNull JBPanel<?> historyTab) {
        historyTab.removeAll();

        final @NotNull JBLabel emptyState = new JBLabel(Bundle.message("view.history.none"), SwingConstants.CENTER);
        emptyState.setForeground(UIUtil.getContextHelpForeground());
        emptyState.setBorder(JBUI.Borders.empty(20));

        historyTab.add(emptyState, BorderLayout.CENTER);

        // Its own, as the details tab's load does its own. A tab that swapped its
        // contents and left them unpainted showed the previous test case's rows
        // until the tester switched away and back (#66, finding 79).
        historyTab.revalidate();
        historyTab.repaint();
    }
}
