package org.testin.ui.dialogs;

import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

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

    public static void setLeadingIcon(final ExtendableTextField textField, final Icon icon) {
        if (icon == null) {
            textField.setExtensions();
            return;
        }

        textField.setExtensions(new ExtendableTextComponent.Extension() {
            @Override
            public Icon getIcon(final boolean hovered) {
                return icon;
            }

            @Override
            public boolean isIconBeforeText() {
                return true;
            }

            @Override
            public int getIconGap() {
                return JBUI.scale(8);
            }
        });
    }

    public static ComponentPopupBuilder createPopupBuilder(final JComponent content,
                                                           final JComponent focusComponent,
                                                           final String title,
                                                           final @Nullable Icon titleIcon) {
        final ComponentPopupBuilder builder = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(content, focusComponent)
                .setTitle(title)
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .setResizable(false)
                .setMinSize(new Dimension(JBUI.scale(350), 0));

        if (titleIcon != null) {
            builder.setTitleIcon(new ActiveIcon(titleIcon));
        }

        return builder;
    }

}
