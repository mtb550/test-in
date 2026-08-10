package org.testin.editorPanel;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.testin.enums.CardHoverAction;
import org.testin.enums.Group;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.FontSync;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class Shared {
    private static final int BADGE_RADIUS = 20;

    public static JBLabel createPriorityBadge(final TestCaseDto tc) {
        return Optional.of(tc.getPriority())
                .map(p -> new RoundedBadge(p.getName(), p.getColor()))
                .orElseGet(() -> new RoundedBadge("Unknown", JBColor.GRAY));
    }

    public static JBLabel createGroupBadge(final Group group) {
        return new RoundedBadge(group.getName(), JBColor.darkGray);
    }

    public static void drawDescriptionActionIcons(final Component c, final Graphics g, final int x, final int y, final String hoveredAction, final boolean isRunning) {
        final int startX = JBUI.scale(16) + x + JBUI.scale(10);

        final Icon navIcon = AllIcons.Nodes.Class;
        final boolean isNavHovered = CardHoverAction.NAVIGATE_TO_TEST_METHOD.name().equals(hoveredAction);
        drawHoverableIcon(c, g, navIcon, startX, y, isNavHovered);

        final int runStartX = startX + navIcon.getIconWidth() + JBUI.scale(8);
        final Icon runIcon = isRunning ? AllIcons.Actions.Suspend : AllIcons.RunConfigurations.TestState.Run;
        final boolean isRunHovered = CardHoverAction.RUN_TEST_CASE.name().equals(hoveredAction);
        drawHoverableIcon(c, g, runIcon, runStartX, y, isRunHovered);
    }

    private static void drawHoverableIcon(final Component c, final Graphics g, final Icon baseIcon, final int x, final int y, final boolean isHovered) {
        if (isHovered) {
            final Icon scaledIcon = IconUtil.scale(baseIcon, c, 1.5f);
            final int offsetX = (scaledIcon.getIconWidth() - baseIcon.getIconWidth()) / 2;
            final int offsetY = (scaledIcon.getIconHeight() - baseIcon.getIconHeight()) / 2;
            scaledIcon.paintIcon(c, g, x - offsetX, y - offsetY);
        } else {
            baseIcon.paintIcon(c, g, x, y);
        }
    }

}