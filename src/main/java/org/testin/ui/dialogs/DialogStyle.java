package org.testin.ui.dialogs;

import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Shared theme-aware content styling for lightweight project popups.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DialogStyle {

    /**
     * No icon: one that paints nothing, in no space. A field or a row that has
     * no icon says so with an icon of its own type rather than with a null the
     * framework and every caller would have to check (#71).
     */
    public static final @NotNull Icon NO_ICON = EmptyIcon.ICON_0;

    public static <T extends JComponent> @NotNull T styleContent(final @NotNull T component) {
        component.setOpaque(true);
        component.setBackground(UIUtil.getPanelBackground());
        return component;
    }

    public static void setLeadingIcon(final @NotNull ExtendableTextField textField, final @NotNull Icon icon) {
        if (icon == NO_ICON) {
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
                                                                    final @NotNull String title) {
        return JBPopupFactory.getInstance()
                .createComponentPopupBuilder(content, focusComponent)
                .setTitle(title)
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                // A click outside never dismisses a dialog - Escape cancels.
                .setCancelOnClickOutside(false)
                .setMovable(false)
                .setResizable(false)
                .setMinSize(new Dimension(JBUI.scale(350), 0));
    }

}
