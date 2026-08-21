package org.testin.testrun;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CollapsiblePanel {

    public static @NotNull JBPanel<?> build(final @NotNull String title, final @NotNull JComponent content, final boolean initiallyVisible) {
        final @NotNull JBPanel<?> wrapper = new JBPanel<>(new BorderLayout());

        final @NotNull JBLabel titleLabel = new JBLabel(title);

        // The hint says what a click does - platform hint style (small,
        // gray, italic) so the popup explains itself at a glance.
        final @NotNull JBLabel hintLabel = new JBLabel();
        hintLabel.setFont(JBUI.Fonts.smallFont().deriveFont(Font.ITALIC));
        hintLabel.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);
        hintLabel.setBorder(JBUI.Borders.emptyLeft(6));

        final @NotNull JBPanel<?> header = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setOpaque(false);
        header.setBorder(JBUI.Borders.empty(4));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.add(titleLabel);
        header.add(hintLabel);

        content.setVisible(initiallyVisible);

        // One place maps the state to arrow and hint - construction and every
        // toggle render through it, so they can never drift apart.
        final @NotNull Runnable syncHeader = () -> {
            final boolean expanded = content.isVisible();
            titleLabel.setIcon(expanded ? AllIcons.General.ArrowDown : AllIcons.General.ArrowRight);
            hintLabel.setText(expanded ? "Collapse" : "Expand");
        };
        syncHeader.run();

        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                content.setVisible(!content.isVisible());
                syncHeader.run();
                wrapper.revalidate();
                wrapper.repaint();
            }
        });

        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(content, BorderLayout.CENTER);

        return wrapper;
    }
}
