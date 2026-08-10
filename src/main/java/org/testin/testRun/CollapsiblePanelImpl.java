package org.testin.testRun;

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
public final class CollapsiblePanelImpl {

    public static JBPanel<?> build(final @NotNull String title, final @NotNull JComponent content, final boolean initiallyVisible) {
        final JBPanel<?> wrapper = new JBPanel<>(new BorderLayout());

        final JBLabel titleLabel = new JBLabel(title, AllIcons.General.ArrowRight, SwingConstants.LEFT);
        titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        titleLabel.setBorder(JBUI.Borders.empty(4));

        content.setVisible(initiallyVisible);

        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                final boolean expanded = !content.isVisible();
                content.setVisible(expanded);
                titleLabel.setIcon(expanded ? AllIcons.General.ArrowDown : AllIcons.General.ArrowRight);
                wrapper.revalidate();
                wrapper.repaint();
            }
        });

        wrapper.add(titleLabel, BorderLayout.NORTH);
        wrapper.add(content, BorderLayout.CENTER);

        return wrapper;
    }
}
