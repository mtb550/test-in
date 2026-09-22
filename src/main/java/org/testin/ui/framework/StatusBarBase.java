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

package org.testin.ui.framework;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.StatusBarItem;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.util.Fonts;

import javax.swing.Icon;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class StatusBarBase {
    private static final @NotNull String INNER_SEPARATOR = " ";
    private static final @NotNull String OUTER_SEPARATOR = "       ";

    private final @NotNull JBPanel<?> statusBar;

    private final @NotNull Color labelColor = JBUI.CurrentTheme.Label.foreground();
    private final @NotNull Color dotColor = JBUI.CurrentTheme.ContextHelp.FOREGROUND;
    private final @NotNull Color separatorColor = JBUI.CurrentTheme.ContextHelp.FOREGROUND;

    private final @NotNull Font font = Fonts.small();

    private final @NotNull Icon icon = AllIcons.General.Keyboard;
    private final @NotNull Border border = JBUI.Borders.emptyRight(6);

    // UC-INTERNAL-007, Rule-INTERNAL-079
    public StatusBarBase(final StatusBarItem @NotNull [] items) {
        this.statusBar = new JBPanel<>(new BorderLayout());
        this.statusBar.setBorder(JBUI.Borders.empty(4, 10));
        this.statusBar.setOpaque(true);
        this.statusBar.setBackground(JBUI.CurrentTheme.Advertiser.background());

        updateItems(items);
        setShown(true);
    }

    // UC-SETTING-008, Rule-SETTING-029
    public void setShown(final boolean wanted) {
        statusBar.setVisible(wanted && Services.getInstance(AppSettingsState.class).showShortcutHints);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-078
    public void updateItems(final StatusBarItem @NotNull [] items) {
        this.statusBar.removeAll();

        final @NotNull JBPanel<?> contentPanel = new JBPanel<>(new GridBagLayout());
        contentPanel.setOpaque(false);

        final @NotNull GridBagConstraints onOneRow = new GridBagConstraints();
        onOneRow.gridy = 0;
        onOneRow.anchor = GridBagConstraints.WEST;

        contentPanel.add(setStatusBarIcon(), onOneRow);

        for (int i = 0; i < items.length; i++) {
            final @NotNull StatusBarItem item = items[i];
            contentPanel.add(Keycap.of(item.getShortcutText()), onOneRow);
            contentPanel.add(createDot(), onOneRow);
            contentPanel.add(createLabel(item.getName()), onOneRow);

            if (i < items.length - 1) {
                contentPanel.add(createSeparator(), onOneRow);
            }
        }

        this.statusBar.add(contentPanel, BorderLayout.WEST);

        this.statusBar.revalidate();
        this.statusBar.repaint();
    }

    private @NotNull JBLabel setStatusBarIcon() {
        final @NotNull JBLabel label = new JBLabel(icon);
        label.setBorder(border);
        return label;
    }

    private @NotNull JBLabel createDot() {
        final @NotNull JBLabel label = new JBLabel(INNER_SEPARATOR);
        label.setForeground(dotColor);
        return label;
    }

    private @NotNull JBLabel createLabel(final @NotNull String text) {
        final @NotNull JBLabel label = new JBLabel(text);
        label.setForeground(labelColor);
        label.setFont(font);
        return label;
    }

    private @NotNull JBLabel createSeparator() {
        final @NotNull JBLabel label = new JBLabel(OUTER_SEPARATOR);
        label.setForeground(separatorColor);
        label.setFont(font);
        return label;
    }

    public @NotNull JBPanel<?> getPanel() {
        return statusBar;
    }
}
