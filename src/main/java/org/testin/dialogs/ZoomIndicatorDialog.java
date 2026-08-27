package org.testin.dialogs;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.services.Services;
import org.testin.ui.dialogs.DialogStyle;

import java.util.Optional;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The "Font size: 14pt" bubble that appears while the tester zooms.
 * <p>
 * One per project, not one per IDE. The popup and the timer that hides it were
 * static, which is one slot for every open project: the bubble is shown against
 * a component in one project's window, and a zoom in the other project cancelled
 * it and restarted a timer the first was still counting on. It was the plugin's
 * last static mutable state of that shape, and a project service is what the
 * rest of the plugin uses - it is disposed with its project, so a popup left
 * open when a project closes goes with it.
 */
@Service(Service.Level.PROJECT)
public final class ZoomIndicatorDialog implements Disposable {

    private final @NotNull Project p;

    /**
     * One indicator at a time in this project: zooming happens in the focused
     * editor, so a new one replaces the previous.
     */
    private @NotNull Optional<JBPopup> currentPopup = Optional.empty();

    /**
     * One reusable timer rather than one per wheel event.
     */
    private final @NotNull Timer hideTimer = new Timer(5000, e -> hide());

    ZoomIndicatorDialog(final @NotNull Project p) {
        this.p = p;
        hideTimer.setRepeats(false);
    }

    /**
     * Shows what the font size has just become, against this component.
     */
    public static void show(final @NotNull Project p, final @NotNull JComponent parent, final float currentSize) {
        Services.getInstance(p, ZoomIndicatorDialog.class).showIn(parent, currentSize);
    }

    private void hide() {
        currentPopup.filter(popup -> !popup.isDisposed()).ifPresent(JBPopup::cancel);
        currentPopup = Optional.empty();
    }

    private void showIn(final @NotNull JComponent parent, final float currentSize) {
        hide();

        if (!parent.isShowing()) return;

        final @NotNull JBPanel<?> panel = new JBPanel<>();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(JBUI.Borders.empty(6, 12));
        DialogStyle.styleContent(panel);

        panel.add(new JBLabel("Font size: " + (int) currentSize + "pt"));
        panel.add(Box.createHorizontalStrut(JBUI.scale(12)));

        final @NotNull JBLabel gearIcon = new JBLabel(AllIcons.General.GearPlain);
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

        final @NotNull JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setCancelOnClickOutside(false)
                .setFocusable(false)
                .setRequestFocus(false)
                .createPopup();
        currentPopup = Optional.of(popup);

        final @NotNull Dimension popupSize = panel.getPreferredSize();
        final @NotNull Rectangle visibleRect = parent.getVisibleRect();
        final int x = visibleRect.x + (visibleRect.width - popupSize.width) / 2;
        final int y = visibleRect.y + visibleRect.height - popupSize.height - JBUI.scale(25);

        popup.show(new RelativePoint(parent, new Point(x, y)));

        hideTimer.restart();
    }

    @Override
    public void dispose() {
        hideTimer.stop();
        hide();
    }
}
