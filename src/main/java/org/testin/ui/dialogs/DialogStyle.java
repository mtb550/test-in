package org.testin.ui.dialogs;

import com.intellij.util.ui.UIUtil;

import javax.swing.*;

/**
 * Shared theme-aware content styling for lightweight project popups.
 */
public final class DialogStyle {

    private DialogStyle() {
    }

    public static <T extends JComponent> T styleContent(final T component) {
        component.setOpaque(true);
        component.setBackground(UIUtil.getPanelBackground());
        return component;
    }

}
