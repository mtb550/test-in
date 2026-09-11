package org.testin.view.details.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditors;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.ui.FontSync;
import org.testin.util.Bundle;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Id extends BaseDetails {

    final int BADGE_ARC_SIZE = 16;
    final int BADGE_BORDER_V = 3;
    final int BADGE_BORDER_H = 10;
    final int FLOW_GAP = 8;
    final int COPY_SUCCESS_DELAY_MS = 1500;
    final @NotNull String COPY_TOOLTIP = Bundle.message("view.id.copy");
    final @NotNull String GO_TOOLTIP = Bundle.message("view.id.go");
    final @NotNull Color BG_COLOR = new JBColor(Gray._230, Gray._80);
    final @NotNull Color FG_COLOR = new JBColor(Gray._130, Gray._170);
    final int INSETS_TOP = 5;
    final int INSETS_LEFT = 16;
    final int INSETS_BOTTOM = 0;
    final int INSETS_RIGHT = 16;

    // UC-VIEW-PANEL-009
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
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

        final float badgeSize = Math.max(8.0f, FontSync.getBaseFontSize() - 3.0f);
        idBadge.setFont(JBFont.label().deriveFont(Font.BOLD, badgeSize));

        idBadge.setForeground(FG_COLOR);
        idBadge.setBorder(JBUI.Borders.empty(BADGE_BORDER_V, BADGE_BORDER_H));
        idBadge.setOpaque(false);

        // UC-VIEW-PANEL-009, Rule-VIEW-PANEL-063.
        //
        // The identity is the link, because it is the one thing on the panel
        // that names exactly one test case.
        //
        // The editor, and not the tree. GoTo is what the search and the path bar
        // call, and it reveals the node in the tree first because for them
        // "where is this" is half the question. It is not half of this one: the
        // tester is looking at the case already and asked for the editor, so
        // moving the tree underneath them is an answer to something nobody
        // asked. openAndSelect is the other half of GoTo, and the one owner of
        // opening a test set on a case.
        idBadge.setToolTipText(GO_TOOLTIP);
        idBadge.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        idBadge.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                Services.getInstance(p, TestinEditors.class).openAndSelect(p, dto.getParent(), dto);
            }
        });

        final @NotNull JBLabel copyIcon = new JBLabel(AllIcons.Actions.Copy);
        copyIcon.setToolTipText(COPY_TOOLTIP);
        copyIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        copyIcon.addMouseListener(new MouseAdapter() {
            // UC-VIEW-PANEL-009, Rule-VIEW-PANEL-039, Rule-VIEW-PANEL-040
            @Override
            public void mouseClicked(final MouseEvent e) {
                CopyPasteManager.getInstance().setContents(new StringSelection(dto.getId().toString()));
                copyIcon.setIcon(AllIcons.General.InspectionsOK);
                final @NotNull Timer timer = new Timer(COPY_SUCCESS_DELAY_MS, evt -> copyIcon.setIcon(AllIcons.Actions.Copy));
                timer.setRepeats(false);
                timer.start();
            }
        });

        final @NotNull JBPanel<?> idContainer = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(FLOW_GAP), 0));
        idContainer.setOpaque(false);
        idContainer.add(idBadge);
        idContainer.add(copyIcon);

        gbc.gridx = 0;
        gbc.gridy = currentRow;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = JBUI.insets(INSETS_TOP, INSETS_LEFT, INSETS_BOTTOM, INSETS_RIGHT);

        panel.add(idContainer, gbc);
        return currentRow + 1;
    }
}