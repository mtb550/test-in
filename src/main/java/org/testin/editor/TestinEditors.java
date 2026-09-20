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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.view.ViewToolWindowFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class TestinEditors {
    private @NotNull Optional<VirtualFile> openFileAt(final @NotNull Project p, final @NotNull Path path) {
        return Arrays.stream(FileEditorManager.getInstance(p).getOpenFiles())
                .filter(open -> open instanceof UnifiedVirtualFile testinFile && testinFile.getDir().getPath().equals(path))
                .findFirst();
    }

    public void reloadOpen(final @NotNull Project p, final @NotNull Path path) {
        editorAt(p, path).ifPresent(TestinEditor::reloadData);
    }

    // UC-TREE-PANEL-011, UC-TREE-PANEL-012, Rule-TREE-PANEL-111, Rule-TREE-PANEL-116
    public void close(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        openFilesUnder(p, dir.getPath()).forEach(FileEditorManager.getInstance(p)::closeFile);
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-111
    public boolean busyUnder(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        return openFilesUnder(p, dir.getPath()).stream()
                .flatMap(open -> Arrays.stream(FileEditorManager.getInstance(p).getAllEditors(open)))
                .anyMatch(tab -> tab instanceof UnifiedFileEditor unified && unified.getEditor().isBusy());
    }

    private @NotNull List<VirtualFile> openFilesUnder(final @NotNull Project p, final @NotNull Path path) {
        return Arrays.stream(FileEditorManager.getInstance(p).getOpenFiles())
                .filter(open -> open instanceof UnifiedVirtualFile testinFile && testinFile.getDir().getPath().startsWith(path))
                .toList();
    }

    // Rule-EDITOR-PANEL-015
    public void closeAll(final @NotNull Project p) {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(p);

        for (final VirtualFile open : fed.getOpenFiles()) {
            if (open instanceof UnifiedVirtualFile) fed.closeFile(open);
        }
    }

    // UC-TREE-PANEL-025, Rule-TREE-PANEL-082
    public void refreshOpen(final @NotNull Project p) {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(p);

        for (final VirtualFile open : fed.getOpenFiles()) {
            if (!(open instanceof UnifiedVirtualFile testinFile)) continue;

            if (!isIndexed(p, testinFile)) {
                Logger.info("Closing the editor for a node that is no longer indexed: " + testinFile.getName());
                fed.closeFile(testinFile);
                continue;
            }

            for (final FileEditor tab : fed.getAllEditors(testinFile)) {
                if (!(tab instanceof UnifiedFileEditor unified)) continue;

                final @NotNull TestinEditor editor = unified.getEditor();
                if (editor.isBusy()) {
                    Logger.info("Leaving a busy editor as it is rather than reloading under the tester: "
                            + testinFile.getName());
                    continue;
                }
                editor.reloadData();
            }
        }
    }

    private boolean isIndexed(final @NotNull Project p, final @NotNull UnifiedVirtualFile file) {
        return Services.getInstance(p, ProjectIndexer.class).nodeExists(file.getDir().getPath());
    }

    public void openThen(final @NotNull Project p, final @NotNull DirectoryDto dir, final @NotNull Consumer<TestinEditor> tell) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!openNow(p, dir, true)) return;

            editorFor(p, dir).ifPresent(tell);
        });
    }

    public void openAndSelect(final @NotNull Project p, final @NotNull DirectoryDto dir, final @NotNull TestCaseDto tc) {
        openThen(p, dir, editor -> {
            editor.selectWhenLoaded(tc.getId());

            ViewToolWindowFactory.showPanel(p, List.of(tc), dir.getPath2());
        });
    }

    public @NotNull Optional<RunEditor> runEditorFor(final @NotNull Project p, final @NotNull TestRunDirectoryDto run) {
        return editorFor(p, run).filter(RunEditor.class::isInstance).map(RunEditor.class::cast);
    }

    public @NotNull Optional<TestinEditor> editorFor(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        return editorAt(p, dir.getPath());
    }

    private @NotNull Optional<TestinEditor> editorAt(final @NotNull Project p, final @NotNull Path path) {
        return openFileAt(p, path).flatMap(open -> Arrays.stream(FileEditorManager.getInstance(p).getAllEditors(open))
                .filter(UnifiedFileEditor.class::isInstance)
                .map(tab -> ((UnifiedFileEditor) tab).getEditor())
                .findFirst());
    }

    public void closeThenOpen(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(p);

        ApplicationManager.getApplication().invokeLater(() -> openFileAt(p, dir.getPath()).ifPresentOrElse(open -> {
            fed.closeFile(open);
            fed.openFile(open, true);
        }, () -> open(p, dir)));
    }

    public void open(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        open(p, dir, true);
    }

    public void open(final @NotNull Project p, final @NotNull DirectoryDto dir, final boolean focus) {
        ApplicationManager.getApplication().invokeLater(() -> openNow(p, dir, focus));
    }

    private boolean openNow(final @NotNull Project p, final @NotNull DirectoryDto dir, final boolean focus) {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(p);

        final @NotNull Optional<VirtualFile> already = openFileAt(p, dir.getPath());
        if (already.isPresent()) {
            Logger.info("Editor already open, focusing: " + dir.getName());
            fed.openFile(already.orElseThrow(), focus);
            return true;
        }

        if (!dir.isOpenableInEditor()) {
            Logger.info("Nothing to open for " + dir.getName() + " - it holds nodes rather than test cases");
            return false;
        }

        Logger.info("Opening Editor: " + dir.getPath());
        fed.openFile(new UnifiedVirtualFile(dir, EditorType.of(dir)), focus);

        return true;
    }

    public @NotNull List<Path> openNodePaths(final @NotNull Project p) {
        final @NotNull List<Path> paths = new ArrayList<>();

        for (final VirtualFile vf : FileEditorManager.getInstance(p).getOpenFiles()) {
            if (vf instanceof UnifiedVirtualFile uvf) paths.add(uvf.getDir().getPath().toAbsolutePath());
        }

        return paths;
    }
}