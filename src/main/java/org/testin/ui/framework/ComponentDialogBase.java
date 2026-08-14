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
     * An input field on its own — the rename-dialog component.
     */
    public static @NotNull TextInputBuilder textField() {
        return new TextInputBuilder();
    }

    /**
     * A plain message — the confirmation-dialog component. The muted From/To
     * rows show where a transfer goes; pass null to omit either.
     */
    public static @NotNull ComponentDialogBase<DialogMessage> message(final @NotNull String text,
                                                                      final @Nullable String from,
                                                                      final @Nullable String to) {
        return new ComponentDialogBase<>(new DialogMessage(text, from, to));
    }

    /**
     * Wraps an application-specific component that implements
     * {@link IDialogComponent} (e.g. a form built by its own class).
     */
    public static <C extends IDialogComponent> @NotNull ComponentDialogBase<C> of(final @NotNull C component) {
        return new ComponentDialogBase<>(component);
    }

    /**
     * A confirm button row — clicking it submits the dialog. For working
     * dialogs where a visible OK button reads better than an Enter hint.
     */
    public static @NotNull ComponentDialogBase<DialogButton> button(final @NotNull String text) {
        return new ComponentDialogBase<>(new DialogButton(text));
    }

    /**
     * Read-only context rows — muted caption + value, display only.
     */
    public static @NotNull DetailsBuilder details() {
        return new DetailsBuilder();
    }

    /**
     * A multi-line text area — e.g. a pasted error or exception.
     */
    public static @NotNull TextAreaBuilder textArea() {
        return new TextAreaBuilder();
    }

    /**
     * A captioned radio row — one radio per option, one always selected.
     */
    public static <T> @NotNull RadioBuilder<T> radios(final @NotNull String caption) {
        return new RadioBuilder<>(caption);
    }

    /**
     * Framework default: every declared icon renders desaturated, so the
     * color accents of tree icons (e.g. badge dots) never distract inside
     * a dialog. Dialogs pass their icons plain.
     */
    private static @Nullable Icon desaturate(final @Nullable Icon icon) {
        return icon == null ? null : IconUtil.desaturate(icon);
    }

    public @NotNull C getComponent() {
        return component;
    }

    /**
     * Fluent builder for {@link DialogDetails}.
     */
    public static final class DetailsBuilder {

        private final @NotNull List<DialogDetails.Row> rows = new ArrayList<>();

        private DetailsBuilder() {
        }

        /**
         * One caption/value row; a null or blank value skips the row.
         */
        public @NotNull DetailsBuilder row(final @NotNull String caption, final @Nullable String value) {
            if (value != null && !value.isBlank()) {
                rows.add(new DialogDetails.Row(caption, value));
            }
            return this;
        }

        public @NotNull ComponentDialogBase<DialogDetails> build() {
            return new ComponentDialogBase<>(new DialogDetails(List.copyOf(rows)));
        }
    }

    /**
     * Fluent builder for {@link RadioSelection}.
     */
    public static final class RadioBuilder<T> {

        private final @NotNull String caption;
        private final @NotNull List<RadioSelection.Option<T>> options = new ArrayList<>();
        private @Nullable T selected;

        private RadioBuilder(final @NotNull String caption) {
            this.caption = caption;
        }

        public @NotNull RadioBuilder<T> option(final @NotNull String name, final @NotNull T value) {
            options.add(new RadioSelection.Option<>(name, value));
            return this;
        }

        /**
         * The initially selected value — must be one of the options.
         */
        public @NotNull RadioBuilder<T> select(final @NotNull T value) {
            this.selected = value;
            return this;
        }

        public @NotNull ComponentDialogBase<RadioSelection<T>> build() {
            if (options.isEmpty()) {
                throw new IllegalStateException("radios needs at least one .option(...)");
            }
            if (selected == null || options.stream().noneMatch(option -> option.value().equals(selected))) {
                throw new IllegalStateException("radios needs .select(...) with one of the declared options");
            }
            return new ComponentDialogBase<>(new RadioSelection<>(caption, List.copyOf(options), selected));
        }
    }

    /**
     * Fluent builder for {@link TextArea}.
     */
    public static final class TextAreaBuilder {

        private @Nullable String placeholder;
        private @Nullable String value;
        private int rows = 5;

        private TextAreaBuilder() {
        }

        public @NotNull TextAreaBuilder placeholder(final @Nullable String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public @NotNull TextAreaBuilder value(final @Nullable String value) {
            this.value = value;
            return this;
        }

        /**
         * Preferred visible rows; the area still grows with the dialog.
         */
        public @NotNull TextAreaBuilder rows(final int rows) {
            this.rows = rows;
            return this;
        }

        public @NotNull ComponentDialogBase<TextArea> build() {
            return new ComponentDialogBase<>(new TextArea(placeholder, value, rows));
        }
    }

    /**
     * Fluent builder for {@link TextInput}.
     */
    public static final class TextInputBuilder {

        private @Nullable Icon icon;
        private @Nullable String placeholder;
        private @Nullable String value;

        private TextInputBuilder() {
        }

        public @NotNull TextInputBuilder icon(final @Nullable Icon icon) {
            this.icon = desaturate(icon);
            return this;
        }

        public @NotNull TextInputBuilder placeholder(final @Nullable String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        /**
         * The value the field opens with (e.g. the current name on rename).
         */
        public @NotNull TextInputBuilder value(final @Nullable String value) {
            this.value = value;
            return this;
        }

        public @NotNull ComponentDialogBase<TextInput> build() {
            return new ComponentDialogBase<>(new TextInput(icon, placeholder, value));
        }
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
