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

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Shortcut-hint strip at the bottom of a window, styled like the platform's
 * popup advertiser bar: tinted background, hairline on top, muted hint text
 * with the keystroke emphasized.
 * <p>
 * Usable as it stands, which is what its name says - {@code Base} is this
 * project's word for a parent that is not abstract. It was abstract anyway,
 * so light mode, which wants exactly this strip and adds nothing to it, would
 * have had to declare an empty subclass to say so (#13).
 */
public class StatusBarBase {
    /**
     * Between a keystroke and its meaning.
     */
    private static final @NotNull String INNER_SEPARATOR = " ";
    /**
     * Between entries — whitespace only, sized to read as a deliberate gap.
     */
    private static final @NotNull String OUTER_SEPARATOR = "       ";

    private final @NotNull JBPanel<?> statusBar;

    // Keystroke and its meaning read clearly in light and dark; only the
    // separators stay muted.
    private final @NotNull Color labelColor = JBUI.CurrentTheme.Label.foreground();
    private final @NotNull Color dotColor = JBUI.CurrentTheme.ContextHelp.FOREGROUND;
    private final @NotNull Color separatorColor = JBUI.CurrentTheme.ContextHelp.FOREGROUND;

    private final @NotNull Font font = JBUI.Fonts.smallFont();

    // A keyboard: says "these are keys".
    private final @NotNull Icon icon = AllIcons.General.Keyboard;
    private final @NotNull Border border = JBUI.Borders.emptyRight(6);

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-079.
     * <p>
     * <b>A surface has one of these.</b> It could be built without its keyboard
     * icon, which existed for one reason: to be the second strip of a pair, so
     * that two stacked rows read as one hint area rather than as two bars. The
     * pair is gone - a dialog two tinted rows tall to say six words was paying
     * for a redraw nobody could see - and with it the only way to build a strip
     * that is half of something (#56, and undone here).
     * <p>
     * So there is one constructor, every strip carries its icon, and a surface
     * that wants more keys puts them on the strip it has.
     */
    public StatusBarBase(final StatusBarItem @NotNull [] items) {
        this.statusBar = new JBPanel<>(new BorderLayout());
        this.statusBar.setBorder(JBUI.Borders.empty(4, 10));
        this.statusBar.setOpaque(true);
        this.statusBar.setBackground(JBUI.CurrentTheme.Advertiser.background());

        updateItems(items);
        setShown(true);
    }

    /**
     * UC-SETTING-008, Rule-SETTING-029.
     * <p>
     * Whether this strip is drawn, with the tester's standing answer folded in.
     * <p>
     * <b>One owner, because there are two questions and one strip.</b> A surface
     * may have its own reason to hide the keys - light mode's view menu is the
     * only one today - and a tester may have said once that they never want to
     * see them. Asked separately at each call site, a dialog would have to know
     * about a setting it has no other business with, and the ones that never ask
     * would quietly ignore it. Asked here, every strip in the plugin obeys the
     * setting without a single dialog being touched.
     * <p>
     * Read as the strip is drawn rather than cached, so turning the setting off
     * takes effect on the next dialog rather than the next IDE.
     */
    public void setShown(final boolean wanted) {
        statusBar.setVisible(wanted && Services.getInstance(AppSettingsState.class).showShortcutHints);
    }

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-078.
     * <p>
     * The strip, as one row.
     * <p>
     * <b>{@link GridBagLayout} rather than a {@link FlowLayout}</b>, which is
     * the same choice the editors' own status bar made and for a neighbouring
     * reason. A flow wraps: given less width than its items need it starts a
     * second row, so a dialog narrower than its own hints - the test case
     * attribute dialogs, and the bulk editor on CTRL+M - drew the strip on two
     * lines and grew a line taller to hold it. Every item sits on {@code gridy
     * 0} here, so there is no second row for it to wrap onto; a strip wider
     * than the dialog is shortened rather than folded.
     */
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
