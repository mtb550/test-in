package org.testin.dialogs;

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
import org.jetbrains.annotations.Nullable;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ZoomIndicatorDialog {

    private static final @NotNull Timer HIDE_TIMER = new Timer(5000, e -> hide());
    /**
     * A single indicator popup is enough: zooming always happens in the focused
     * editor, so a new indicator replaces the previous one. One reusable timer
     * instead of allocating one per wheel event; hide() clears the reference so
     * nothing dangles after the popup is gone.
     */
    private static @Nullable JBPopup currentPopup;

    static {
        HIDE_TIMER.setRepeats(false);
    }

    private static void hide() {
        if (currentPopup != null && !currentPopup.isDisposed()) currentPopup.cancel();
        currentPopup = null;
    }

    public static void show(final @NotNull Project p, final @NotNull JComponent parent, final float currentSize) {
        hide();

        if (!parent.isShowing()) return;

        final JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(JBUI.Borders.empty(6, 12));
        DialogStyle.styleContent(panel);

        panel.add(new JBLabel("Font size: " + (int) currentSize + "pt"));
        panel.add(Box.createHorizontalStrut(JBUI.scale(12)));

        final JBLabel gearIcon = new JBLabel(AllIcons.General.GearPlain);
        gearIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gearIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final @NotNull MouseEvent e) {
                hide();
                if (!p.isDisposed()) {
                    ShowSettingsUtilImpl.showSettingsDialog(p, "preferences.editor", "Change font size");
                }
            }
        });
        panel.add(gearIcon);

        final JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setCancelOnClickOutside(false)
                .setFocusable(false)
                .setRequestFocus(false)
                .createPopup();
        currentPopup = popup;

        final Dimension popupSize = panel.getPreferredSize();
        final Rectangle visibleRect = parent.getVisibleRect();
        final int x = visibleRect.x + (visibleRect.width - popupSize.width) / 2;
        final int y = visibleRect.y + visibleRect.height - popupSize.height - JBUI.scale(25);

        popup.show(new RelativePoint(parent, new Point(x, y)));

        HIDE_TIMER.restart();
    }
}
