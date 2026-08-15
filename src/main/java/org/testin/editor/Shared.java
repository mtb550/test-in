package org.testin.editor;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.CardHoverAction;
import org.testin.enums.Group;
import org.testin.model.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.util.Optional;

public class Shared {

    public static @NotNull JBLabel createPriorityBadge(final @NotNull TestCaseDto tc) {
        return Optional.of(tc.getPriority())
                .map(p -> new RoundedBadge(p.getName(), p.getColor()))
                .orElseGet(() -> new RoundedBadge("Unknown", JBColor.GRAY));
    }

    public static @NotNull JBLabel createGroupBadge(final @NotNull Group group) {
        return new RoundedBadge(group.getName(), JBColor.darkGray);
    }

    public static void drawDescriptionActionIcons(final @NotNull Component c, final @NotNull Graphics g, final int x, final int y, final @Nullable String hoveredAction, final boolean isRunning) {
        final int startX = JBUI.scale(16) + x + JBUI.scale(10);

        final Icon navIcon = AllIcons.Nodes.Class;
        final boolean isNavHovered = CardHoverAction.NAVIGATE_TO_TEST_METHOD.name().equals(hoveredAction);
        drawHoverableIcon(c, g, navIcon, startX, y, isNavHovered);

        final int runStartX = startX + navIcon.getIconWidth() + JBUI.scale(8);
        final Icon runIcon = isRunning ? AllIcons.Actions.Suspend : AllIcons.RunConfigurations.TestState.Run;
        final boolean isRunHovered = CardHoverAction.RUN_TEST_CASE.name().equals(hoveredAction);
        drawHoverableIcon(c, g, runIcon, runStartX, y, isRunHovered);
    }

    /**
     * Hands a wheel event to the enclosing scroll pane, so a component that does
     * not scroll itself does not swallow the gesture. Ctrl/Meta is left alone -
     * that is the font zoom, not a scroll.
     */
    public static void forwardWheelToScrollPane(final @NotNull MouseWheelEvent e) {
        if (e.isControlDown() || e.isMetaDown())
            return;

        final JBScrollPane scrollPane = findScrollPane(e.getComponent());

        if (scrollPane != null && e.getComponent() != scrollPane) {
            final MouseWheelEvent clonedEvent = (MouseWheelEvent) SwingUtilities.convertMouseEvent(e.getComponent(), e, scrollPane);
            scrollPane.dispatchEvent(clonedEvent);
            e.consume();
        }
    }

    private static @Nullable JBScrollPane findScrollPane(final @Nullable Component component) {
        Component current = component;
        while (current != null) {
            if (current instanceof JBScrollPane)
                return (JBScrollPane) current;

            current = current.getParent();
        }

        return null;
    }

    private static void drawHoverableIcon(final @NotNull Component c, final @NotNull Graphics g, final @NotNull Icon baseIcon, final int x, final int y, final boolean isHovered) {
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