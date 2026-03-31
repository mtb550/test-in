package testGit.viewPanel.details.components;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.actions.NavigateToCode;
import testGit.actions.RunTestCase;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ActionIcons extends BaseDetails {

    @Override
    public int render(@NotNull JBPanel<?> panel, @NotNull GridBagConstraints gbc, @NotNull TestCaseDto dto, int currentRow) {
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionsPanel.setOpaque(false);

        Icon navIcon = AllIcons.Nodes.Class;
        JLabel navLabel = new JLabel(navIcon);
        navLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        navLabel.setToolTipText("Navigate to Code");

        int navTargetWidth = (int) (navIcon.getIconWidth() * 1.5f);
        int navTargetHeight = (int) (navIcon.getIconHeight() * 1.5f);
        navLabel.setPreferredSize(new Dimension(navTargetWidth, navTargetHeight));
        navLabel.setHorizontalAlignment(SwingConstants.CENTER);
        navLabel.setVerticalAlignment(SwingConstants.CENTER);

        navLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                navLabel.setIcon(IconUtil.scale(navIcon, navLabel, 1.5f));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                navLabel.setIcon(navIcon);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                NavigateToCode.execute(dto);
            }
        });

        Icon runIcon = AllIcons.RunConfigurations.TestState.Run;
        JLabel runLabel = new JLabel(runIcon);
        runLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runLabel.setToolTipText("Run Test Case");

        int runTargetWidth = (int) (runIcon.getIconWidth() * 1.5f);
        int runTargetHeight = (int) (runIcon.getIconHeight() * 1.5f);
        runLabel.setPreferredSize(new Dimension(runTargetWidth, runTargetHeight));
        runLabel.setHorizontalAlignment(SwingConstants.CENTER);
        runLabel.setVerticalAlignment(SwingConstants.CENTER);

        runLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                runLabel.setIcon(IconUtil.scale(runIcon, runLabel, 1.5f));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                runLabel.setIcon(runIcon);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                RunTestCase.execute(dto);
            }
        });

        actionsPanel.add(navLabel);
        actionsPanel.add(Box.createHorizontalStrut(JBUI.scale(4)));
        actionsPanel.add(runLabel);

        gbc.gridx = 0;
        gbc.gridy = currentRow;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(0, 16, 8, 16);
        panel.add(actionsPanel, gbc);

        return currentRow + 1;
    }
}