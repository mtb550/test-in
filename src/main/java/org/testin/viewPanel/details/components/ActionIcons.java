package org.testin.viewPanel.details.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.NavigateToCode;
import org.testin.pojo.CardHoverAction;
import org.testin.pojo.RunStatus;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ActionIcons extends BaseDetails {
    final float BASE_SCALE = 1.3f;
    final float HOVER_SCALE = 1.8f;
    final int STRUT_WIDTH = 8;
    final int INSETS_TOP = 8;
    final int INSETS_LEFT = 16;
    final int INSETS_BOTTOM = 0;
    final int INSETS_RIGHT = 16;
    final Icon navIconRaw = AllIcons.Nodes.Class;

    public ActionIcons() {
    }

    @Override
    public int render(final @NotNull Project project, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        final JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionsPanel.setOpaque(false);

        final JLabel navLabel = new JLabel();
        final Icon navIconBase = IconUtil.scale(navIconRaw, navLabel, BASE_SCALE);
        final Icon navIconHover = IconUtil.scale(navIconRaw, navLabel, HOVER_SCALE);
        navLabel.setIcon(navIconBase);
        navLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        new HelpTooltip()
                .setDescription(HtmlChunk.text(CardHoverAction.NAVIGATE.getTooltip()))
                .setShortcut(KeyboardSet.NavigateToCode.getShortcutText())
                .installOn(navLabel);

        final int navTargetWidth = (int) (navIconRaw.getIconWidth() * HOVER_SCALE);
        final int navTargetHeight = (int) (navIconRaw.getIconHeight() * HOVER_SCALE);
        navLabel.setPreferredSize(new Dimension(navTargetWidth, navTargetHeight));
        navLabel.setHorizontalAlignment(SwingConstants.CENTER);
        navLabel.setVerticalAlignment(SwingConstants.CENTER);

        navLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                navLabel.setIcon(navIconHover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                navLabel.setIcon(navIconBase);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                new NavigateToCode(null).execute(project, dto);
            }
        });

        final JLabel runLabel = new JLabel();
        final RunStatus currentStatus = RunStatus.fromString(dto.getTempStatus());
        final Icon currentRunIconRaw = currentStatus.getIcon();
        final Icon runIconBase = IconUtil.scale(currentRunIconRaw, runLabel, BASE_SCALE);
        final Icon runIconHover = IconUtil.scale(currentRunIconRaw, runLabel, HOVER_SCALE);
        runLabel.setIcon(runIconBase);
        runLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        new HelpTooltip()
                .setDescription(HtmlChunk.text(currentStatus.getTooltip()))
                .setShortcut(KeyboardSet.RunTestCase.getShortcutText())
                .installOn(runLabel);

        final int runTargetWidth = (int) (currentRunIconRaw.getIconWidth() * HOVER_SCALE);
        final int runTargetHeight = (int) (currentRunIconRaw.getIconHeight() * HOVER_SCALE);
        runLabel.setPreferredSize(new Dimension(runTargetWidth, runTargetHeight));
        runLabel.setHorizontalAlignment(SwingConstants.CENTER);
        runLabel.setVerticalAlignment(SwingConstants.CENTER);

        runLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                runLabel.setIcon(runIconHover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                runLabel.setIcon(runIconBase);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                currentStatus.executeAction(project, dto, null);
            }
        });

        actionsPanel.add(navLabel);
        actionsPanel.add(Box.createHorizontalStrut(JBUI.scale(STRUT_WIDTH)));
        actionsPanel.add(runLabel);

        gbc.gridx = 0;
        gbc.gridy = currentRow;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(INSETS_TOP, INSETS_LEFT, INSETS_BOTTOM, INSETS_RIGHT);
        panel.add(actionsPanel, gbc);

        return currentRow + 1;
    }
}