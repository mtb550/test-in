package org.testin.ui.framework;

import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The typed content part of a framework dialog. Each component type gets its
 * own fluent builder here, so a dialog class declares its content as one flat
 * chain; new component types are added as new builders without touching
 * existing dialogs.
 */
public final class ComponentDialogBase<C extends IDialogComponent> {

    private final @NotNull C component;

    private ComponentDialogBase(final @NotNull C component) {
        this.component = component;
    }

    /**
     * An input field over a selection list — the create-dialog component.
     * One {@code .selection(...)} call per row:
     * <pre>
     * ComponentDialogBase.&lt;DirectoryType&gt;textFieldWithSelections()
     *         .icon(...)
     *         .placeholder("set name..")
     *         .selection(icon, "Test Set", "Holds test cases", DirectoryType.TS)
     *         .build()
     * </pre>
     */
    public static <T> @NotNull TextFieldBuilder<T> textFieldWithSelections() {
        return new TextFieldBuilder<>();
    }

    /**
     * Wraps any future component that implements {@link IDialogComponent}.
     */
    public static <C extends IDialogComponent> @NotNull ComponentDialogBase<C> of(final @NotNull C component) {
        return new ComponentDialogBase<>(component);
    }

    public @NotNull C getComponent() {
        return component;
    }

    /**
     * Fluent builder for {@link TextFieldWithSelections}.
     */
    public static final class TextFieldBuilder<T> {

        private final @NotNull List<SelectionList<T>> selections = new ArrayList<>();
        private @Nullable Icon icon;
        private @Nullable String placeholder;

        private TextFieldBuilder() {
        }

        /**
         * Framework default: every declared icon renders desaturated, so the
         * color accents of tree icons (e.g. badge dots) never distract inside
         * a dialog. Dialogs pass their icons plain.
         */
        private static @Nullable Icon desaturate(final @Nullable Icon icon) {
            return icon == null ? null : IconUtil.desaturate(icon);
        }

        /**
         * The field's leading icon before a selection takes over.
         */
        public @NotNull TextFieldBuilder<T> icon(final @Nullable Icon icon) {
            this.icon = desaturate(icon);
            return this;
        }

        public @NotNull TextFieldBuilder<T> placeholder(final @Nullable String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        /**
         * One selectable row: icon, name, muted hint, and the submitted value.
         */
        public @NotNull TextFieldBuilder<T> selection(final @Nullable Icon icon, final @NotNull String name,
                                                      final @Nullable String hint, final @NotNull T value) {
            selections.add(SelectionList.add(desaturate(icon), name, hint, value));
            return this;
        }

        public @NotNull ComponentDialogBase<TextFieldWithSelections<T>> build() {
            if (selections.isEmpty()) {
                throw new IllegalStateException("textFieldWithSelections needs at least one .selection(...)");
            }
            return new ComponentDialogBase<>(new TextFieldWithSelections<>(icon, placeholder, List.copyOf(selections)));
        }
    }
}
