package org.testin.ui.dialogs;

import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import java.util.List;
import java.util.ArrayList;
import com.intellij.util.ui.NamedColorUtil;
import com.intellij.util.ui.JBFont;
import com.intellij.ui.TextIcon;
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

    /**
     * UC-INTERNAL-001, Rule-INTERNAL-073.
     * <p>
     * Everything drawn inside a field but outside its text: the icon in front,
     * and one short note at the end.
     * <p>
     * Both at once, because {@code setExtensions} replaces the lot. Set
     * separately, whichever ran last wiped the other - and the icon is set again
     * every time the selection moves, so a note would have survived until the
     * first arrow key.
     *
     * @param note said at the right of the field, empty for no note. Drawn on
     *             the field's own background so it reads as a word in the field
     *             rather than a chip on it
     */
    public static void setDecorations(final @NotNull ExtendableTextField textField, final @NotNull Icon icon, final @NotNull String note) {
        final @NotNull List<ExtendableTextComponent.Extension> extensions = new ArrayList<>();

        if (icon != NO_ICON) extensions.add(leading(icon));
        if (!note.isEmpty()) extensions.add(trailing(note, textField));

        textField.setExtensions(extensions);
    }

    private static @NotNull ExtendableTextComponent.Extension leading(final @NotNull Icon icon) {
        return new ExtendableTextComponent.Extension() {
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
        };
    }

    /**
     * The note, as the platform's own text-drawing icon - so the font metrics,
     * the scaling and the painting are its problem rather than ours.
     * <p>
     * The background is the field's, which is what makes the rounded rectangle
     * {@code TextIcon} draws invisible: what is wanted here is a word, not a
     * chip.
     */
    private static @NotNull ExtendableTextComponent.Extension trailing(final @NotNull String note, final @NotNull ExtendableTextField textField) {
        final @NotNull TextIcon drawn = new TextIcon(note, NamedColorUtil.getInactiveTextColor(), textField.getBackground(), JBUI.scale(2));
        drawn.setFont(JBFont.label());
        drawn.setWithBorders(false);

        return new ExtendableTextComponent.Extension() {
            @Override
            public @NotNull Icon getIcon(final boolean hovered) {
                return drawn;
            }

            @Override
            public int getIconGap() {
                return JBUI.scale(8);
            }
        };
    }

    public static @NotNull ComponentPopupBuilder createPopupBuilder(final @NotNull JComponent content, final @NotNull JComponent focusComponent, final @NotNull String title) {
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
