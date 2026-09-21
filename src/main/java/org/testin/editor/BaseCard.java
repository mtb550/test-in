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

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Badges;
import org.testin.model.Automated;
import org.testin.ui.framework.Prose;
import org.testin.ui.framework.RowStripe;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class BaseCard extends JBPanel<BaseCard> {
    protected final @NotNull JTextArea titleArea = Prose.of("");
    protected final @NotNull JBPanel<?> badgePanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(10), 0));
    protected final @NotNull Map<String, JBLabel> attributeLabels = new HashMap<>();
    protected final @NotNull JBPanel<?> content = new JBPanel<>(new VerticalLayout(JBUI.scale(4)));
    protected final @NotNull BorderLayoutPanel wrapper = new BorderLayoutPanel();
    protected boolean isRowHovered;
    protected @NotNull String hoveredAction = "";
    @Setter
    private @NotNull List<CardHoverAction.Offered> hoverButtons = List.of();
    protected @NotNull Automated automation = Automated.UNKNOWN;
    private @NotNull String plainTitle = "";
    private int titleColumnWidth = Integer.MAX_VALUE;
    private int titleWidth;

    // Rule-CODEGEN-082
    protected final @NotNull Project p;

    public BaseCard(final @NotNull Project p) {
        this.p = p;
        setLayout(new BorderLayout());
        setOpaque(true);

        titleArea.setForeground(UIUtil.getLabelForeground());

        badgePanel.setOpaque(false);
        badgePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final @NotNull JBPanel<?> titleLine = new JBPanel<>();
        titleLine.setLayout(new BoxLayout(titleLine, BoxLayout.X_AXIS));
        titleLine.setOpaque(false);
        titleLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLine.add(titleArea);

        content.setOpaque(false);
        content.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(titleLine);
        content.add(badgePanel);

        wrapper.setOpaque(false);
        wrapper.setBorder(JBUI.Borders.empty(12, 16));
        wrapper.addToCenter(content);

        add(wrapper, BorderLayout.CENTER);
    }

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-014
    public static @NotNull String titleText(final int position, final boolean showOrder, final @NotNull String description) {
        final @NotNull String order = showOrder ? String.format(Locale.ENGLISH, "%d.", position) : "";
        final @NotNull String title = description.trim();

        return order.isEmpty() || title.isEmpty() ? order + title : order + " " + title;
    }

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-003
    public void applyListLayout(final @NotNull JList<?> list) {
        final @NotNull Font listFont = list.getFont();
        final float baseSize = listFont.getSize2D();

        titleArea.setFont(CardTitle.titleFont(list));

        for (final JBLabel lbl : attributeLabels.values()) {
            lbl.setFont(listFont.deriveFont(baseSize));
        }

        final float badgeSize = Math.max(8.0f, baseSize - 2.0f);
        for (final Component c : badgePanel.getComponents()) {
            c.setFont(listFont.deriveFont(Font.BOLD, badgeSize));
        }

        titleColumnWidth = CardTitle.titleColumnWidth(list.getWidth(), hoverButtons.size());
        titleWidth = CardTitle.titleWidth(list, plainTitle, hoverButtons.size());
        layOutTitle();
    }

    private void layOutTitle() {
        titleArea.setText(plainTitle);
        titleArea.setSize(Math.min(titleColumnWidth, Short.MAX_VALUE), Short.MAX_VALUE);
    }

    protected void updateUI(final int index, final @NotNull String title, final @NotNull List<Badges.Badge> badges, final @NotNull Map<String, String> details) {
        plainTitle = title;

        setBackground(RowStripe.of(index));
        setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0));

        Badges.showBadges(badgePanel, badges);

        attributeLabels.values().forEach(lbl -> lbl.setVisible(false));

        details.forEach((attrName, value) -> {
            if (value.isBlank()) return;

            final @NotNull JBLabel lbl = attributeLabels.computeIfAbsent(attrName, k -> {
                final @NotNull JBLabel newLbl = createDetailLabel();
                content.add(newLbl);
                return newLbl;
            });

            lbl.setText(attrName + ": " + value);
            lbl.setVisible(true);
        });

        badgePanel.revalidate();
        badgePanel.repaint();
    }

    public void setActionsState(final boolean isSelected, final boolean isRowHovered, final @NotNull String hoveredAction) {
        this.isRowHovered = isRowHovered;
        this.hoveredAction = hoveredAction;
        if (isSelected) {
            setBackground(EditorColors.SELECTION_BACKGROUND);
        }
    }

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-195
    @Override
    protected void paintChildren(final Graphics g) {
        super.paintChildren(g);
        if (isRowHovered) {
            CardTitle.drawDescriptionActionIcons(this, g, titleWidth, hoveredAction, hoverButtons, automation);
        }
    }

    private @NotNull JBLabel createDetailLabel() {
        final @NotNull JBLabel label = new JBLabel();
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
