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

    private static final float BASE_SCALE = 1.3f;
    private static final float HOVER_SCALE = 1.8f;
    private static final int STRUT_WIDTH = 8;
    private static final int INSETS_TOP = 8;
    private static final int INSETS_LEFT = 16;
    private static final int INSETS_BOTTOM = 0;
    private static final int INSETS_RIGHT = 16;
    private static final String NAVIGATE_TOOLTIP = "Navigate to Code";
    private static final String RUN_TOOLTIP = "Run Test Case";
    private static final Icon navIconRaw = AllIcons.Nodes.Class;
    private static final Icon runIconRaw = AllIcons.RunConfigurations.TestState.Run;
    private static final Icon suspend = AllIcons.Actions.Suspend;
    private static final Icon testPassed = AllIcons.RunConfigurations.TestPassed;
    private static final Icon testFailed = AllIcons.RunConfigurations.TestFailed;

    @Override
    public int render(@NotNull final JBPanel<?> panel, @NotNull final GridBagConstraints gbc, @NotNull final TestCaseDto dto, final int currentRow) {
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionsPanel.setOpaque(false);

        JLabel navLabel = new JLabel();

        Icon navIconBase = IconUtil.scale(navIconRaw, navLabel, BASE_SCALE);
        Icon navIconHover = IconUtil.scale(navIconRaw, navLabel, HOVER_SCALE);

        navLabel.setIcon(navIconBase);
        navLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        navLabel.setToolTipText(NAVIGATE_TOOLTIP);

        int navTargetWidth = (int) (navIconRaw.getIconWidth() * HOVER_SCALE);
        int navTargetHeight = (int) (navIconRaw.getIconHeight() * HOVER_SCALE);
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

        JLabel runLabel = new JLabel();

        Icon currentRunIconRaw = runIconRaw;
        if ("RUNNING".equals(dto.getTempStatus()))
            currentRunIconRaw = suspend;
        else if ("PASSED".equals(dto.getTempStatus()))
            currentRunIconRaw = testPassed;
        else if ("FAILED".equals(dto.getTempStatus()))
            currentRunIconRaw = testFailed;

        Icon runIconBase = IconUtil.scale(currentRunIconRaw, runLabel, BASE_SCALE);
        Icon runIconHover = IconUtil.scale(currentRunIconRaw, runLabel, HOVER_SCALE);

        runLabel.setIcon(runIconBase);
        runLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runLabel.setToolTipText(RUN_TOOLTIP);

        int runTargetWidth = (int) (currentRunIconRaw.getIconWidth() * HOVER_SCALE);
        int runTargetHeight = (int) (currentRunIconRaw.getIconHeight() * HOVER_SCALE);
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
                if (!"RUNNING".equals(dto.getTempStatus()))
                    RunTestCase.execute(dto);
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