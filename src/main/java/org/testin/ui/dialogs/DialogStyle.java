package org.testin.ui.dialogs;

import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Shared theme-aware content styling for lightweight project popups.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DialogStyle {

    public static <T extends JComponent> @NotNull T styleContent(final @NotNull T component) {
        component.setOpaque(true);
        component.setBackground(UIUtil.getPanelBackground());
        return component;
    }

    public static void setLeadingIcon(final @NotNull ExtendableTextField textField, final @Nullable Icon icon) {
        if (icon == null) {
            textField.setExtensions();
            return;
        }

        textField.setExtensions(new ExtendableTextComponent.Extension() {
            @Override
            public @NotNull Icon getIcon(final boolean hovered) {
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

    public static @NotNull ComponentPopupBuilder createPopupBuilder(final @NotNull JComponent content,
                                                                    final @NotNull JComponent focusComponent,
                                                                    final @NotNull String title,
                                                                    final @Nullable Icon titleIcon) {
        final ComponentPopupBuilder builder = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(content, focusComponent)
                .setTitle(title)
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                // A click outside never dismisses a dialog - Escape cancels.
                .setCancelOnClickOutside(false)
                .setMovable(false)
                .setResizable(false)
                .setMinSize(new Dimension(JBUI.scale(350), 0));

        if (titleIcon != null) {
            builder.setTitleIcon(new ActiveIcon(titleIcon));
        }

        return builder;
    }

}
