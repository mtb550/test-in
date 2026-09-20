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

// UC-SHARE-018, Rule-SHARE-081
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FieldName {
    static @NotNull String of(final @NotNull String jsonField) {
        final int dot = jsonField.indexOf('.');
        if (dot < 0) return ofTestCaseField(jsonField);

        return ofRunKey(jsonField.substring(dot + 1), jsonField);
    }

    private static @NotNull String ofTestCaseField(final @NotNull String jsonField) {
        final @NotNull String constant = jsonField.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);

        for (final TestEditorAttributes attribute : TestEditorAttributes.values()) {
            if (attribute.name().equals(constant)) return attribute.getName();
        }

        return jsonField;
    }

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
