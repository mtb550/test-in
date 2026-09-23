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

import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.editor.test.TestEditor;
import org.testin.model.DirectoryType;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.util.Bundle;

import javax.swing.Icon;
import java.util.function.BiFunction;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class EditorType extends FakeFileType {
    // UC-EDITOR-PANEL-001
    public static final @NotNull EditorType TEST_RUN = new EditorType(
            "Test Run",
            Bundle.message("editor.type.run.description"),
            DirectoryType.TR.getIcon(),
            RunEditor::new
    );

    public static final @NotNull EditorType TEST_CASE = new EditorType(
            "Test Case",
            Bundle.message("editor.type.case.description"),
            DirectoryType.TS.getIcon(),
            TestEditor::new
    );

    private final @NotNull String name;
    private final @NotNull String description;
    private final @NotNull Icon icon;
    private final @NotNull BiFunction<Project, UnifiedVirtualFile, TestinEditor> factory;

    // UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-001
    public static @NotNull EditorType of(final @NotNull DirectoryDto dir) {
        return dir.getType() == DirectoryType.TR ? TEST_RUN : TEST_CASE;
    }

    @Override
    public boolean isMyFileType(final @NotNull VirtualFile file) {
        return false;
    }
}
