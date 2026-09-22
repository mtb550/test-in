/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.ui.framework;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ComponentDialogBase<C extends DialogComponent> {
    @Getter
    private final @NotNull C component;

    public static <T> @NotNull TextFieldBuilder<T> textFieldWithSelections() {
        return new TextFieldBuilder<>();
    }

    public static @NotNull TextInputBuilder textField() {
        return new TextInputBuilder();
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-221
    public static @NotNull ComponentDialogBase<SpellCheckedField> spellCheckedField(final @NotNull Project p, final @NotNull String caption, final @NotNull String placeholder, final @NotNull String value) {
        return new ComponentDialogBase<>(new SpellCheckedField(p, caption, placeholder, value));
    }

    public static @NotNull ComponentDialogBase<DialogMessage> message(final @NotNull String text) {
        return message(text, "", "");
    }

    public static @NotNull ComponentDialogBase<DialogMessage> message(final @NotNull String text, final @NotNull String from, final @NotNull String to) {
        return new ComponentDialogBase<>(new DialogMessage(text, from, to));
    }

    public static <C extends DialogComponent> @NotNull ComponentDialogBase<C> of(final @NotNull C component) {
        return new ComponentDialogBase<>(component);
    }

    public static @NotNull ComponentDialogBase<DialogSplitButton> splitButton(final @NotNull String... labels) {
        return new ComponentDialogBase<>(new DialogSplitButton(List.of(labels)));
    }

    public static @NotNull ComponentDialogBase<DialogButton> button(final @NotNull String text) {
        return new ComponentDialogBase<>(new DialogButton(text));
    }

    public static @NotNull DetailsBuilder details() {
        return new DetailsBuilder();
    }

    public static @NotNull TextAreaBuilder textArea() {
        return new TextAreaBuilder();
    }

    public static @NotNull ComponentDialogBase<Picture> picture(final byte @NotNull [] png) {
        return new ComponentDialogBase<>(new Picture(png));
    }

    public static @NotNull ComponentDialogBase<ChoiceInput> choice(final @NotNull String caption, final @NotNull List<String> options, final @NotNull String selected) {
        return new ComponentDialogBase<>(new ChoiceInput(caption, List.copyOf(options), selected));
    }

    public static <T> @NotNull RadioBuilder<T> radios(final @NotNull String caption) {
        return new RadioBuilder<>(caption);
    }

    public static @NotNull TableBuilder table() {
        return new TableBuilder();
    }

    public static final class TableBuilder {
        private final @NotNull List<String> columns = new ArrayList<>();
        private final @NotNull List<Integer> widths = new ArrayList<>();

        public @NotNull TableBuilder column(final @NotNull String heading, final int width) {
            columns.add(heading);
            widths.add(width);
            return this;
        }

        public @NotNull ComponentDialogBase<SelectionTable> build() {
            return new ComponentDialogBase<>(new SelectionTable(List.copyOf(columns), List.copyOf(widths)));
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class DetailsBuilder {
        private final @NotNull List<DialogDetails.Row> rows = new ArrayList<>();

        public @NotNull DetailsBuilder row(final @NotNull String caption, final @NotNull String value) {
            if (!value.isBlank()) {
                rows.add(new DialogDetails.Row(caption, Optional.empty(), value));
            }
            return this;
        }

        public @NotNull DetailsBuilder row(final @NotNull Icon icon, final @NotNull String value) {
            if (!value.isBlank()) {
                rows.add(new DialogDetails.Row("", Optional.of(icon), value));
            }
            return this;
        }

        public @NotNull ComponentDialogBase<DialogDetails> build() {
            return new ComponentDialogBase<>(new DialogDetails(List.copyOf(rows)));
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class RadioBuilder<T> {
        private final @NotNull String caption;
        private final @NotNull List<RadioSelection.Option<T>> options = new ArrayList<>();
        private @NotNull Optional<T> selected = Optional.empty();

        public @NotNull RadioBuilder<T> option(final @NotNull String name, final @NotNull T value) {
            options.add(new RadioSelection.Option<>(name, value));
            return this;
        }

        public @NotNull RadioBuilder<T> options(final @NotNull List<T> values, final @NotNull Function<T, String> name) {
            values.forEach(value -> option(name.apply(value), value));
            return this;
        }

        public @NotNull RadioBuilder<T> select(final @NotNull T value) {
            this.selected = Optional.of(value);
            return this;
        }

        public @NotNull ComponentDialogBase<RadioSelection<T>> build() {
            if (options.isEmpty()) {
                throw new IllegalStateException("radios needs at least one .option(...)");
            }
            if (options.stream().noneMatch(option -> selected.filter(option.value()::equals).isPresent())) {
                throw new IllegalStateException("radios needs .select(...) with one of the declared options");
            }
            return new ComponentDialogBase<>(new RadioSelection<>(caption, List.copyOf(options), selected.orElseThrow()));
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class TextAreaBuilder {
        private @NotNull String caption = "";
        private @NotNull String placeholder = "";
        private @NotNull String value = "";
        private int rows = 5;
        private boolean acceptsImages = false;
        private @NotNull List<byte[]> images = List.of();

        public @NotNull TextAreaBuilder caption(final @NotNull String caption) {
            this.caption = caption;
            return this;
        }

        public @NotNull TextAreaBuilder placeholder(final @NotNull String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public @NotNull TextAreaBuilder value(final @NotNull String value) {
            this.value = value;
            return this;
        }

        public @NotNull TextAreaBuilder rows(final int rows) {
            this.rows = rows;
            return this;
        }

        public @NotNull TextAreaBuilder images(final @NotNull List<byte[]> images) {
            this.acceptsImages = true;
            this.images = images;
            return this;
        }

        public @NotNull ComponentDialogBase<TextArea> build() {
            return new ComponentDialogBase<>(new TextArea(caption, placeholder, value, rows, acceptsImages, images));
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class TextInputBuilder {
        private @NotNull Icon icon = DialogStyle.NO_ICON;
        private @NotNull String placeholder = "";
        private @NotNull String value = "";
        private @NotNull String accepts = TextInput.ANYTHING;

        public @NotNull TextInputBuilder icon(final @NotNull Icon icon) {
            this.icon = icon;
            return this;
        }

        public @NotNull TextInputBuilder placeholder(final @NotNull String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public @NotNull TextInputBuilder value(final @NotNull String value) {
            this.value = value;
            return this;
        }

        public @NotNull TextInputBuilder accepting(final @NotNull String regex) {
            this.accepts = regex;
            return this;
        }

        public @NotNull ComponentDialogBase<TextInput> build() {
            return new ComponentDialogBase<>(new TextInput(icon, placeholder, value, accepts));
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class TextFieldBuilder<T> {
        private static final int SEARCH_ROWS = 12;

        private final @NotNull List<SelectionList<T>> selections = new ArrayList<>();
        private @NotNull Optional<Rows<T>> rows = Optional.empty();
        private @NotNull Icon icon = DialogStyle.NO_ICON;
        private @NotNull String placeholder = "";

        public @NotNull TextFieldBuilder<T> icon(final @NotNull Icon icon) {
            this.icon = icon;
            return this;
        }

        public @NotNull TextFieldBuilder<T> placeholder(final @NotNull String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public @NotNull TextFieldBuilder<T> selection(final @NotNull Icon icon, final @NotNull String name, final @NotNull String hint, final @NotNull T value) {
            selections.add(SelectionList.add(icon, name, hint, value));
            return this;
        }

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

            final @NotNull List<SelectionList<T>> fixed = List.copyOf(selections);
            final @NotNull Rows.Answer<T> always = Rows.Answer.of(fixed);

            return new ComponentDialogBase<>(
                    new TextFieldWithSelections<>(icon, placeholder, fixed, _ -> always, fixed.size()));
        }
    }
}
