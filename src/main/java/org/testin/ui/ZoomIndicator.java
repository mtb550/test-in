package org.testin.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ZoomIndicator {
    private static JBPopup currentPopup;
    private static Timer hideTimer;

    public static void show(final @NotNull Project project, final JComponent parent, float currentSize) {
        if (currentPopup != null && !currentPopup.isDisposed()) currentPopup.cancel();

        JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(JBUI.Borders.empty(6, 12));
        panel.setOpaque(true);

        panel.add(new JBLabel("Font size: " + (int) currentSize + "pt"));
        panel.add(Box.createHorizontalStrut(JBUI.scale(12)));

        JBLabel gearIcon = new JBLabel(AllIcons.General.GearPlain);
        gearIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gearIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (currentPopup != null) currentPopup.cancel();
                if (!project.isDisposed()) {
                    ShowSettingsUtilImpl.showSettingsDialog(project, "preferences.editor", "Change font size");
                }
            }
        });
        panel.add(gearIcon);

        currentPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setCancelOnClickOutside(false)
                .setFocusable(false)
                .setRequestFocus(false)
                .createPopup();

        Dimension popupSize = panel.getPreferredSize();
        Rectangle visibleRect = parent.getVisibleRect();
        int x = visibleRect.x + (visibleRect.width - popupSize.width) / 2;
        int y = visibleRect.y + visibleRect.height - popupSize.height - JBUI.scale(25);

        currentPopup.show(new RelativePoint(parent, new Point(x, y)));

        if (hideTimer != null) hideTimer.stop();

        hideTimer = new Timer(5000, e -> {
            if (currentPopup != null && !currentPopup.isDisposed())
                currentPopup.cancel();
        });

        hideTimer.setRepeats(false);
        hideTimer.start();
    }
}