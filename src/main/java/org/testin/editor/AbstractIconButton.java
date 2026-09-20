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

package org.testin.editor;

import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltip;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Icons;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;

public abstract class AbstractIconButton extends JButton {
    private final @NotNull Icon restIcon;
    private final @NotNull Icon zoomedIcon;

    private boolean hovered;

    private boolean on;

    private @NotNull Optional<String> shortcutText = Optional.empty();

    public AbstractIconButton(final @NotNull String tooltip, final @NotNull Icon icon) {
        super(null, icon);
        setToolTipText(tooltip.isEmpty() ? null : tooltip);
        setFocusable(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        setOpaque(false);

        this.restIcon = icon;
        this.zoomedIcon = Icons.zoomStandardIcon(icon, this);

        setIcon(Icons.zoomStandardIcon(AllIcons.Actions.Refresh, this));
        final @NotNull Dimension size = getPreferredSize();
        setIcon(restIcon);

        setDisabledIcon(IconLoader.getDisabledIcon(restIcon));

        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                setHovered(true);
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                setHovered(false);
            }
        });
    }

    public AbstractIconButton(final @NotNull String tooltip, final @NotNull Icon icon, final @NotNull Shortcuts shortcut) {
        this(tooltip, icon, shortcut.getShortcutText());
    }

    public AbstractIconButton(final @NotNull String tooltip, final @NotNull Icon icon, final @NotNull String shortcutText) {
        this("", icon);

        this.shortcutText = Optional.of(shortcutText);
        describe(tooltip);
    }

    protected final void describe(final @NotNull String text) {
        shortcutText.ifPresentOrElse(key -> {
            HelpTooltip.dispose(this);
            new HelpTooltip()
                    .setDescription(HtmlChunk.text(text))
                    .setShortcut(key)
                    .installOn(this);
        }, () -> setToolTipText(text.isEmpty() ? null : text));
    }

    @Override
    protected void fireActionPerformed(final @NotNull ActionEvent event) {
        final @NotNull AnAction clicked = new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                swingClick(event);
            }
        };

        ActionUtil.performAction(clicked, AnActionEvent.createEvent(clicked,
                DataManager.getInstance().getDataContext(this),
                clicked.getTemplatePresentation().clone(),
                ActionPlaces.TOOLBAR, ActionUiKind.TOOLBAR, null));
    }

    private void swingClick(final @NotNull ActionEvent event) {
        super.fireActionPerformed(event);
    }

    private void setHovered(final boolean isHovered) {
        this.hovered = isHovered;
        setIcon(isHovered ? zoomedIcon : restIcon);
        repaint();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);

        if (!enabled && hovered) setHovered(false);
    }

    public void setOn(final boolean isOn) {
        if (on == isOn) return;

        this.on = isOn;
        repaint();
    }

    @Override
    protected void paintComponent(final @NotNull Graphics g) {
        g.setColor(Optional.ofNullable(getParent()).map(Container::getBackground).orElseGet(this::getBackground));
        g.fillRect(0, 0, getWidth(), getHeight());

        if ((on || hovered) && isEnabled()) {
            final @NotNull Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(on
                        ? JBUI.CurrentTheme.ActionButton.pressedBackground()
                        : JBUI.CurrentTheme.ActionButton.hoverBackground());
                final int arc = JBUI.scale(6);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            } finally {
                g2.dispose();
            }
        }

        super.paintComponent(g);
    }
}
