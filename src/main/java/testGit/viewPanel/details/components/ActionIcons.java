package testGit.viewPanel.details.components;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.actions.NavigateToCode;
import testGit.pojo.RunStatus;
import testGit.pojo.dto.TestCaseDto;

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
    final String NAVIGATE_TOOLTIP = "Navigate to Code";
    final Icon navIconRaw = AllIcons.Nodes.Class;

    @Override
    public int render(@NotNull final JBPanel<?> panel, @NotNull final GridBagConstraints gbc, @NotNull final TestCaseDto dto, final int currentRow) {
        final JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionsPanel.setOpaque(false);

        final JLabel navLabel = new JLabel();
        final Icon navIconBase = IconUtil.scale(navIconRaw, navLabel, BASE_SCALE);
        final Icon navIconHover = IconUtil.scale(navIconRaw, navLabel, HOVER_SCALE);
        navLabel.setIcon(navIconBase);
        navLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        navLabel.setToolTipText(NAVIGATE_TOOLTIP);
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
                NavigateToCode.execute(dto);
            }
        });

        final JLabel runLabel = new JLabel();
        final RunStatus currentStatus = RunStatus.fromString(dto.getTempStatus());
        final Icon currentRunIconRaw = currentStatus.getIcon();
        final Icon runIconBase = IconUtil.scale(currentRunIconRaw, runLabel, BASE_SCALE);
        final Icon runIconHover = IconUtil.scale(currentRunIconRaw, runLabel, HOVER_SCALE);
        runLabel.setIcon(runIconBase);
        runLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runLabel.setToolTipText(currentStatus.getTooltip());
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
                currentStatus.executeAction(dto);
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