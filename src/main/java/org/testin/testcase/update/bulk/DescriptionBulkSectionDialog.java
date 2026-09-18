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

package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;
import org.testin.util.NameSanitizer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class DescriptionBulkSectionDialog extends JsonSplitBulkSectionDialog {

    public DescriptionBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems, final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p, selectedItems, updatedItems);
    }

    @Override
    protected @NotNull TestEditorAttributes attribute() {
        return TestEditorAttributes.DESCRIPTION;
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return Bundle.message("bulk.title.description");
    }

    @Override
    protected @NotNull String getJsonFieldName() {
        return "description";
    }

    @Override
    protected boolean showsDescriptionContext() {
        return false;
    }

    @Override
    protected boolean acceptsBlank() {
        return false;
    }

    /**
     * UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-224.
     * <p>
     * The rows whose new description the update dialog would have refused: one
     * that cannot name a Java method, and one that names the same method as
     * another test case in its test set.
     * <p>
     * This dialog asked neither, so three cards given one description came out
     * as three methods with one name, the class stopped compiling, and every
     * case in that test set stopped running - while the tester read
     * <i>Updated 3</i> (#66, finding 159).
     * <p>
     * <b>Against what stays, and then against each other.</b> A case outside
     * this dialog keeps its description, and so does a row the tester left
     * alone; those names are taken before any edited row is looked at. Each
     * edited row then takes its own name in turn, so two rows given one
     * description clash with each other - and two rows that swap descriptions
     * do not, because neither old name is kept.
     */
    @Override
    protected @NotNull Set<Integer> clashing(final @NotNull List<TestCaseDto> items, final @NotNull List<EditedValue> newValues) {
        final @NotNull Set<UUID> inThisDialog = items.stream().map(TestCaseDto::getId).collect(Collectors.toSet());
        final @NotNull Map<Path, Set<String>> taken = new HashMap<>();

        for (int i = 0; i < items.size(); i++) {
            final @NotNull TestCaseDto tc = items.get(i);
            final @NotNull Set<String> inItsSet = taken.computeIfAbsent(tc.getParent().getPath(), path -> keysOutside(path, inThisDialog));

            if (!newValues.get(i).changed()) keyOf(tc.getDescription()).ifPresent(inItsSet::add);
        }

        final @NotNull Set<Integer> clashing = new LinkedHashSet<>();
        final @NotNull List<String> notANameYet = new ArrayList<>();
        final @NotNull List<String> takenYet = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            final @NotNull EditedValue edited = newValues.get(i);
            if (!edited.changed() || edited.value().isEmpty()) continue;

            final @NotNull String methodName = NameSanitizer.methodName(edited.value());

            if (!NameSanitizer.canMakeMethodName(edited.value())) {
                clashing.add(i);
                notANameYet.add(methodName);
                continue;
            }

            if (!taken.get(items.get(i).getParent().getPath()).add(NameSanitizer.methodKey(methodName))) {
                clashing.add(i);
                takenYet.add(methodName);
            }
        }

        // The words the update dialog already uses, so a refusal reads the same
        // whichever door the description came through.
        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);
        if (!notANameYet.isEmpty()) {
            notifier.softRefuse(p, Bundle.message("description.not.a.method.title"), Bundle.message("description.not.a.method.message", notANameYet.getFirst()));
        }
        if (!takenYet.isEmpty()) {
            notifier.softRefuse(p, Bundle.message("description.taken.title"), Bundle.message("description.taken.message", takenYet.getFirst()));
        }

        return clashing;
    }

    /**
     * The method names the test set's other cases already hold - every case in
     * it that this dialog is not editing.
     */
    private @NotNull Set<String> keysOutside(final @NotNull Path testSet, final @NotNull Set<UUID> inThisDialog) {
        final @NotNull Set<String> keys = new HashSet<>();

        Services.getInstance(p, ProjectIndexer.class).getTestCasesForTestSet(testSet).stream()
                .filter(sibling -> !inThisDialog.contains(sibling.getId()))
                .forEach(sibling -> keyOf(sibling.getDescription()).ifPresent(keys::add));

        return keys;
    }

    /**
     * The key of the method a description names, and nothing for one that names
     * none - a case with no description holds no name another can take.
     */
    private static @NotNull Optional<String> keyOf(final @NotNull String description) {
        return Optional.of(NameSanitizer.methodName(description))
                .filter(name -> !name.isEmpty())
                .map(NameSanitizer::methodKey);
    }
}
