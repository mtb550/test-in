package org.testin.ui.framework;

import com.intellij.util.IconUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.dialogs.DialogStyle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The typed content part of a framework dialog. Each component type gets its
 * own fluent builder here, so a dialog class declares its content as one flat
 * chain; new component types are added as new builders without touching
 * existing dialogs.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ComponentDialogBase<C extends DialogComponent> {

    private final @NotNull C component;

    /**
     * An input field over a selection list — the create-dialog component.
     * One {@code .selection(...)} call per row:
     * <pre>
     * ComponentDialogBase.&lt;DirectoryType&gt;textFieldWithSelections()
     *         .icon(...)
     *         .placeholder("set name...")
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
     * A plain message on its own — the ordinary confirmation.
     */
    public static @NotNull ComponentDialogBase<DialogMessage> message(final @NotNull String text) {
        return message(text, "", "");
    }

    /**
     * A message with the muted From/To rows that show where a transfer goes.
     * An empty side is a side the message does not mention.
     */
    public static @NotNull ComponentDialogBase<DialogMessage> message(final @NotNull String text, final @NotNull String from, final @NotNull String to) {
        return new ComponentDialogBase<>(new DialogMessage(text, from, to));
    }

    /**
     * Wraps an application-specific component that implements
     * {@link DialogComponent} (e.g. a form built by its own class).
     */
    public static <C extends DialogComponent> @NotNull ComponentDialogBase<C> of(final @NotNull C component) {
        return new ComponentDialogBase<>(component);
    }

    /**
     * A button with alternatives behind an arrow: the first label is the
     * default, the rest sit under it. For a dialog whose one action has a
     * second, less common form - commit, or commit and push.
     */
    public static @NotNull ComponentDialogBase<DialogSplitButton> splitButton(final @NotNull String... labels) {
        return new ComponentDialogBase<>(new DialogSplitButton(List.of(labels)));
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
     * A captioned row offering existing values and accepting a new one — the
     * open-set counterpart of {@link #radios}. The selected value must be one
     * of the options; what the tester types over it need not be.
     */
    public static @NotNull ComponentDialogBase<ChoiceInput> choice(final @NotNull String caption, final @NotNull List<String> options, final @NotNull String selected) {
        return new ComponentDialogBase<>(new ChoiceInput(caption, List.copyOf(options), selected));
    }

    /**
     * A captioned radio row — one radio per option, one always selected.
     */
    public static <T> @NotNull RadioBuilder<T> radios(final @NotNull String caption) {
        return new RadioBuilder<>(caption);
    }

    /**
     * A read-only table the tester selects rows in — "here is what changed,
     * pick the ones you mean". One {@code .column(...)} call per column:
     * <pre>
     * ComponentDialogBase.table()
     *         .column("Change Type", 120)
     *         .column("Description", 240)
     *         .build()
     * </pre>
     */
    public static @NotNull TableBuilder table() {
        return new TableBuilder();
    }

    /**
     * Framework default: every declared icon renders desaturated, so the
     * color accents of tree icons (e.g. badge dots) never distract inside
     * a dialog. Dialogs pass their icons plain.
     */
    private static @NotNull Icon desaturate(final @NotNull Icon icon) {
        return icon == DialogStyle.NO_ICON ? icon : IconUtil.desaturate(icon);
    }

    public @NotNull C getComponent() {
        return component;
    }

    /**
     * Fluent builder for {@link SelectionTable}.
     */
    public static final class TableBuilder {

        private final @NotNull List<String> columns = new ArrayList<>();
        private final @NotNull List<Integer> widths = new ArrayList<>();

        /**
         * One column: its heading, and the width it prefers before scaling.
         */
        public @NotNull TableBuilder column(final @NotNull String heading, final int width) {
            columns.add(heading);
            widths.add(width);
            return this;
        }

        public @NotNull ComponentDialogBase<SelectionTable> build() {
            return new ComponentDialogBase<>(new SelectionTable(List.copyOf(columns), List.copyOf(widths)));
        }
    }

    /**
     * Fluent builder for {@link DialogDetails}.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class DetailsBuilder {

        private final @NotNull List<DialogDetails.Row> rows = new ArrayList<>();

        /**
         * One caption/value row; a blank value skips the row.
         */
        public @NotNull DetailsBuilder row(final @NotNull String caption, final @NotNull String value) {
            if (!value.isBlank()) {
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
            // A builder that was never given a selection matches no option
            // either, so one test covers both mistakes.
            if (options.stream().noneMatch(option -> option.value().equals(selected))) {
                throw new IllegalStateException("radios needs .select(...) with one of the declared options");
            }
            return new ComponentDialogBase<>(new RadioSelection<>(caption, List.copyOf(options), selected));
        }
    }

    /**
     * Fluent builder for {@link TextArea}.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class TextAreaBuilder {

        private @NotNull String placeholder = "";
        private @NotNull String value = "";
        private int rows = 5;

        public @NotNull TextAreaBuilder placeholder(final @NotNull String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public @NotNull TextAreaBuilder value(final @NotNull String value) {
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
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class TextInputBuilder {

        private @NotNull Icon icon = DialogStyle.NO_ICON;
        private @NotNull String placeholder = "";
        private @NotNull String value = "";
        private @NotNull String accepts = TextInput.ANYTHING;

        public @NotNull TextInputBuilder icon(final @NotNull Icon icon) {
            this.icon = desaturate(icon);
            return this;
        }

        public @NotNull TextInputBuilder placeholder(final @NotNull String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        /**
         * The value the field opens with (e.g. the current name on rename).
         */
        public @NotNull TextInputBuilder value(final @NotNull String value) {
            this.value = value;
            return this;
        }

        /**
         * What the field may hold, as a regular expression the whole value must
         * match - {@code [0-9]*} for a number, {@code [A-Z]{2,4}} for a code.
         * <p>
         * Enforced as the text arrives rather than checked on submit, so the
         * field cannot be made to hold anything else in the first place.
         * Emptying it is always allowed; whether empty is acceptable is the
         * dialog's decision.
         */
        public @NotNull TextInputBuilder accepting(final @NotNull String regex) {
            this.accepts = regex;
            return this;
        }

        public @NotNull ComponentDialogBase<TextInput> build() {
            return new ComponentDialogBase<>(new TextInput(icon, placeholder, value, accepts));
        }
    }

    /**
     * Fluent builder for {@link TextFieldWithSelections}.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class TextFieldBuilder<T> {

        /**
         * How many rows a searching picker shows before it scrolls. Enough to
         * scan without moving the eye, and few enough that the dialog is not the
         * whole screen.
         */
        private static final int SEARCH_ROWS = 12;

        private final @NotNull List<SelectionList<T>> selections = new ArrayList<>();
        private @NotNull Optional<Rows<T>> rows = Optional.empty();
        private @NotNull Icon icon = DialogStyle.NO_ICON;
        private @NotNull String placeholder = "";

        /**
         * The field's leading icon before a selection takes over.
         */
        public @NotNull TextFieldBuilder<T> icon(final @NotNull Icon icon) {
            this.icon = desaturate(icon);
            return this;
        }

        public @NotNull TextFieldBuilder<T> placeholder(final @NotNull String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        /**
         * One selectable row: icon, name, muted hint, and the submitted value.
         */
        public @NotNull TextFieldBuilder<T> selection(final @NotNull Icon icon, final @NotNull String name, final @NotNull String hint, final @NotNull T value) {
            selections.add(SelectionList.add(desaturate(icon), name, hint, value));
            return this;
        }

        /**
         * Rows that answer to what the tester has typed, for a picker that
         * searches rather than one that offers a fixed set (#29).
         */
        public @NotNull TextFieldBuilder<T> rows(final @NotNull Rows<T> rows) {
            this.rows = Optional.of(rows);
            return this;
        }

        public @NotNull ComponentDialogBase<TextFieldWithSelections<T>> build() {
            if (rows.isPresent()) {
                return new ComponentDialogBase<>(
                        new TextFieldWithSelections<>(icon, placeholder, List.of(), rows.orElseThrow(), SEARCH_ROWS));
            }

            if (selections.isEmpty()) {
                throw new IllegalStateException(
                        "textFieldWithSelections needs at least one .selection(...) or a .rows(...)");
            }

            // A fixed set is rows that ignore the query, so there is one way to
            // hold rows rather than two, and one of them declared as a special
            // case of the other.
            final @NotNull List<SelectionList<T>> fixed = List.copyOf(selections);
            return new ComponentDialogBase<>(
                    new TextFieldWithSelections<>(icon, placeholder, fixed, query -> fixed, fixed.size()));
        }
    }
}

