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

package org.testin.git;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.ResultAnalysis;
import org.testin.model.TestRunConfiguration;
import org.testin.testcase.TestEditorAttributes;

import java.util.Locale;

/**
 * UC-SHARE-018, Rule-SHARE-081.
 * <p>
 * What a merge calls one field, in the words the tester already knows it by.
 * <p>
 * One owner, because a merge says the name twice: on the row that asks about a
 * field, and in the line that says what was settled without asking. Those two
 * disagreed - a question row read {@code configuration.PLATFORM} while the
 * settled line beside it said <b>Platform</b> - which is one field looking like
 * two things in one window (#305).
 * <p>
 * Nothing here spells a name. The three enums that already name these values for
 * the editor, the creation dialog and the details panel are asked, so a name
 * changed in one of them changes here too; a key none of them carries keeps its
 * own, because the merge works on the file and a file may hold more than the
 * model does.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FieldName {

    /**
     * The field as the tester knows it.
     *
     * @param jsonField a test case's field, {@code updatedAt}, or a key inside a
     *                  run's marker, {@code configuration.PLATFORM}
     */
    static @NotNull String of(final @NotNull String jsonField) {
        final int dot = jsonField.indexOf('.');
        if (dot < 0) return ofTestCaseField(jsonField);

        return ofRunKey(jsonField.substring(dot + 1), jsonField);
    }

    /**
     * A test case's field, named by the enum the editor, the details panel and
     * the importer are all named from.
     */
    private static @NotNull String ofTestCaseField(final @NotNull String jsonField) {
        final @NotNull String constant = jsonField.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);

        for (final TestEditorAttributes attribute : TestEditorAttributes.values()) {
            if (attribute.name().equals(constant)) return attribute.getName();
        }

        return jsonField;
    }

    /**
     * A key inside a run's marker: the question the tester answered when the run
     * was created, or the heading they wrote their analysis under.
     */
    private static @NotNull String ofRunKey(final @NotNull String key, final @NotNull String jsonField) {
        for (final TestRunConfiguration question : TestRunConfiguration.values()) {
            if (question.name().equals(key)) return question.getDisplayName();
        }
        for (final ResultAnalysis heading : ResultAnalysis.values()) {
            if (heading.name().equals(key)) return heading.getLabel();
        }

        return jsonField;
    }
}
