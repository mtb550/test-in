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

package org.testin.editor.open;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.editor.testcase.TestCaseEditor;
import org.testin.model.DirectoryType;
import org.testin.model.node.DirectoryDto;
import org.testin.util.Bundle;

import java.util.Map;

@AllArgsConstructor
public enum EditorKind {
    // UC-EDITOR-PANEL-001
    TEST(
            "test",
            new EditorType("Test Case", Bundle.message("editor.type.test.case.description"), DirectoryType.TS.getIcon(), TestCaseEditor::new)
    ),

    RUN(
            "run",
            new EditorType("Test Run", Bundle.message("editor.type.test.run.description"), DirectoryType.TR.getIcon(), TestRunEditor::new)
    );

    // Rule-EDITOR-PANEL-001
    private static final @NotNull Map<DirectoryType, EditorKind> OPENS = Map.of(
            DirectoryType.TS, TEST,
            DirectoryType.TR, RUN);

    private final @NotNull String word;
    @Getter
    private final @NotNull EditorType fileType;

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-001
    public static @NotNull EditorKind of(final @NotNull DirectoryDto dir) {
        return OPENS.getOrDefault(dir.getType(), TEST);
    }

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-022
    public @NotNull String detailsKey(final int version) {
        return "testin.selectedDetails." + word + ".v" + version;
    }

    // UC-EDITOR-PANEL-004, Rule-EDITOR-PANEL-026
    public @NotNull String columnWidthKey(final @NotNull Object header) {
        return "testin.grid.colWidth." + word + "." + header;
    }
}
