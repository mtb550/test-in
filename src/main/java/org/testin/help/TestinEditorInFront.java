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


package org.testin.help;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.UnifiedVirtualFile;

import javax.swing.JComponent;
import java.util.Arrays;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TestinEditorInFront {
    // UC-INTERNAL-009, Rule-INTERNAL-126
    static void showOnlyWithIt(final @NotNull Project p, final @NotNull Disposable owner, final @NotNull JComponent component) {
        component.setVisible(isTestinEditor(Arrays.stream(FileEditorManager.getInstance(p).getSelectedFiles()).findFirst()));
        p.getMessageBus().connect(owner).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void selectionChanged(final @NotNull FileEditorManagerEvent event) {
                component.setVisible(isTestinEditor(Optional.ofNullable(event.getNewFile())));
            }
        });
    }

    private static boolean isTestinEditor(final @NotNull Optional<VirtualFile> inFront) {
        return inFront.filter(UnifiedVirtualFile.class::isInstance).isPresent();
    }
}
