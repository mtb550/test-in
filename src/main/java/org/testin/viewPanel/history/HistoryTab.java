package org.testin.viewPanel.history;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class HistoryTab {

    /// TODO: real change history is not implemented yet; show an honest empty state
    /// instead of demo data until it is.
    public void load(final @NotNull JBPanel<?> historyTab) {
        historyTab.removeAll();

        final JBLabel emptyState = new JBLabel("No history available yet", SwingConstants.CENTER);
        emptyState.setForeground(UIUtil.getContextHelpForeground());
        emptyState.setBorder(JBUI.Borders.empty(20));

        historyTab.add(emptyState, BorderLayout.CENTER);
    }
}
