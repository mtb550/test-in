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
package org.testin.view;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.ShownFields;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.TestCaseEditorAttributes;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.view.details.DetailsTab;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Drawn {

    public static @NotNull JBPanel<?> detailsTab(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull Optional<TestRunItems> runItem, final @NotNull List<String> currentPath) {
        final @NotNull Set<TestCaseEditorAttributes> inTestSets = ShownFields.inTestSets();
        final @NotNull Set<TestRunEditorAttributes> inTestRuns = ShownFields.inTestRuns();
        ShownFields.write(ShownFields.IN_TEST_SETS, EnumSet.allOf(TestCaseEditorAttributes.class));
        ShownFields.write(ShownFields.IN_TEST_RUNS, EnumSet.allOf(TestRunEditorAttributes.class));
        try {
            return detailsTabAsChosen(p, tc, runItem, currentPath);
        } finally {
            ShownFields.write(ShownFields.IN_TEST_SETS, inTestSets);
            ShownFields.write(ShownFields.IN_TEST_RUNS, inTestRuns);
        }
    }

    public static @NotNull JBPanel<?> detailsTabAsChosen(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull Optional<TestRunItems> runItem, final @NotNull List<String> currentPath) {
        final @NotNull JBPanel<?> tab = new JBPanel<>();
        new DetailsTab().load(p, tab, Optional.of(tc), runItem, currentPath);
        return tab;
    }

    public static @NotNull List<Component> components(final @NotNull Container container) {
        final @NotNull List<Component> found = new ArrayList<>();
        for (final Component child : container.getComponents()) {
            found.add(child);
            if (child instanceof final Container inner) found.addAll(components(inner));
        }
        return found;
    }

    public static @NotNull String text(final @NotNull Component component) {
        final String text = switch (component) {
            case final AbstractButton button -> button.getText();
            case final JLabel label -> label.getText();
            case final JTextComponent area -> area.getText();
            default -> "";
        };
        return Objects.requireNonNullElse(text, "").trim();
    }

    public static @NotNull List<String> words(final @NotNull Container container) {
        return components(container).stream().map(Drawn::text).filter(text -> !text.isEmpty()).toList();
    }

    public static boolean holds(final @NotNull List<String> words, final @NotNull String text) {
        return words.stream().anyMatch(word -> word.contains(text));
    }

    public static @NotNull Component reading(final @NotNull Container container, final @NotNull String text) {
        return components(container).stream()
                .filter(component -> text(component).equals(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("nothing on the panel reads \"" + text + "\": " + words(container)));
    }
}
