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

package org.testin.editor;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * The two kinds of editor, and the keys their remembered view settings are
 * kept under in the IDE's properties: the grid's column widths and the Details
 * checkboxes (#312, N20).
 * <p>
 * One owner because the words were spelled in four places - the two grids and
 * the two Details buttons - and a kind renamed in one of them would have read
 * nothing back.
 * <p>
 * <b>The words are stored.</b> Every tester's properties already hold keys like
 * {@code testin.grid.colWidth.test.Description} and
 * {@code testin.selectedDetails.run.v7}, so changing a word, or the shape of a
 * key, throws away what those testers set. A constant may be renamed; its word
 * may not. {@code EditorKindTest} pins the strings.
 */
@AllArgsConstructor
public enum EditorKind {

    TEST(
            "test"
    ),

    RUN(
            "run"
    );

    private final @NotNull String word;

    /**
     * UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-022.
     * <p>
     * Where this kind's Details checkboxes are remembered. The version is the
     * button's, bumped only when a stored selection would answer an older
     * question.
     */
    public @NotNull String detailsKey(final int version) {
        return "testin.selectedDetails." + word + ".v" + version;
    }

    /**
     * UC-EDITOR-PANEL-004, Rule-EDITOR-PANEL-026.
     * <p>
     * Where a grid column's width is remembered for this kind, by the column's
     * header.
     */
    public @NotNull String columnWidthKey(final @NotNull Object header) {
        return "testin.grid.colWidth." + word + "." + header;
    }
}
