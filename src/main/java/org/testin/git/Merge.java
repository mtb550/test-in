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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Mapper;

import java.util.List;

/**
 * What came out of merging the three versions Git keeps of one conflicted file,
 * whatever kind of file it was: a test case merged field by field
 * ({@link TestCaseMerge}), one case's result merged as one verdict
 * ({@link RunItemMerge}), or a run's own facts merged by rule
 * ({@link RunMarkerMerge}).
 * <p>
 * One shape for the three, because the tester meets one dialog and the rebase
 * takes one answer: what to stage, what to ask about, and what was decided
 * without asking (#305, G9).
 *
 * @param merged    the file as it will be staged
 * @param questions what both sides changed differently and no rule settles, in
 *                  the order the file lists them
 * @param settled   what both sides changed that was decided without asking -
 *                  reported so a tester knows a choice was made on their behalf,
 *                  which is the half that was missing (#261)
 */
record Merge(@NotNull ObjectNode merged, @NotNull List<Question> questions, @NotNull List<String> settled) {

    public boolean isSettled() {
        return questions.isEmpty();
    }

    /**
     * UC-SHARE-018.
     * <p>
     * Takes the tester's answer for one question: their side stays unless the
     * answer was to take the other, and a question about a value inside an
     * object - a run's configuration - is written inside that object.
     */
    public void answer(final @NotNull Mapper mapper, final @NotNull Question question, final boolean takeTheirs, final @NotNull String theirs) {
        if (!takeTheirs) return;

        final int dot = question.field().indexOf('.');
        if (dot < 0) {
            merged.set(question.field(), mapper.readTree(theirs).path(question.field()).deepCopy());
            return;
        }

        final @NotNull String object = question.field().substring(0, dot);
        final @NotNull String key = question.field().substring(dot + 1);
        final @NotNull JsonNode value = mapper.readTree(theirs).path(object).path(key);

        // withObjectProperty, not with: the one-argument with(String) reads its
        // argument as a JSON Pointer when it starts with a slash, which is why
        // Jackson deprecated it, and the plugin verifier fails the build for a
        // deprecated platform or bundled API.
        merged.withObjectProperty(object).set(key, value.deepCopy());
    }

    /**
     * One thing two testers disagreed about.
     *
     * @param field  the JSON name, which is the field name a tester reads in the
     *               editor; a value inside an object is {@code object.key}
     * @param mine   the value on this machine, as text
     * @param theirs the value the remote brought, as text
     */
    public record Question(@NotNull String field, @NotNull String mine, @NotNull String theirs) {
    }
}
