package org.testin.view.history;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

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

        final @NotNull JBLabel emptyState = new JBLabel("No history available yet", SwingConstants.CENTER);
        emptyState.setForeground(UIUtil.getContextHelpForeground());
        emptyState.setBorder(JBUI.Borders.empty(20));

        historyTab.add(emptyState, BorderLayout.CENTER);
    }
}
