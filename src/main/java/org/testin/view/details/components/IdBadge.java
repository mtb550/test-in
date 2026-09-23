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

package org.testin.view.details.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;
import org.testin.util.Fonts;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class IdBadge {
    private static final int BADGE_ARC_SIZE = 16;
    private static final int BADGE_BORDER_V = 3;
    private static final int BADGE_BORDER_H = 10;
    private static final int FLOW_GAP = 8;
    private static final int COPY_SUCCESS_DELAY_MS = 1500;
    private static final @NotNull String COPY_TOOLTIP = Bundle.message("view.id.copy");
    private static final @NotNull Color BG_COLOR = new JBColor(Gray._230, Gray._80);
    private static final @NotNull Color FG_COLOR = new JBColor(Gray._130, Gray._170);
    // UC-VIEW-PANEL-009, Rule-VIEW-PANEL-041
    public static @NotNull JComponent of(final @NotNull TestCaseDto dto) {
        final @NotNull JBLabel idBadge = new JBLabel(dto.getId().toString()) {
            @Override
            protected void paintComponent(final Graphics g) {
                final @NotNull Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), BADGE_ARC_SIZE, BADGE_ARC_SIZE);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        idBadge.setFont(Fonts.badge());

        idBadge.setForeground(FG_COLOR);
        idBadge.setBorder(JBUI.Borders.empty(BADGE_BORDER_V, BADGE_BORDER_H));
        idBadge.setOpaque(false);

        final @NotNull JBLabel copyIcon = new JBLabel(AllIcons.Actions.Copy);
        copyIcon.setToolTipText(COPY_TOOLTIP);
        copyIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        copyIcon.addMouseListener(new MouseAdapter() {
            // UC-VIEW-PANEL-009, Rule-VIEW-PANEL-039, Rule-VIEW-PANEL-040
            @Override
            public void mouseClicked(final MouseEvent e) {
                CopyPasteManager.getInstance().setContents(new StringSelection(dto.getId().toString()));
                copyIcon.setIcon(AllIcons.General.InspectionsOK);
                final @NotNull Timer timer = new Timer(COPY_SUCCESS_DELAY_MS, _ -> copyIcon.setIcon(AllIcons.Actions.Copy));
                timer.setRepeats(false);
                timer.start();
            }
        });

        final @NotNull JBPanel<?> idContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(FLOW_GAP), 0));
        idContainer.setOpaque(false);
        idContainer.add(idBadge);
        idContainer.add(copyIcon);

        return idContainer;
    }
}