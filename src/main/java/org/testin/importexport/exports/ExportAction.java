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

package org.testin.importexport.exports;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.testcase.TestCaseSnapshot;
import org.testin.testcase.TestEditorAttributes;
import org.testin.testcase.TestEditorAttributes.Can;
import org.testin.ui.dialogs.DestinationForm;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExportAction extends DumbAwareAction {
    public static final @NotNull String NAME = Bundle.message("export.action.name");

    // UC-SHARE-002, Rule-SHARE-001
    private static @NotNull String unreadableWarning(final @NotNull List<String> unreadable) {
        final @NotNull String named = String.join(", ", unreadable.subList(0, Math.min(5, unreadable.size())));
        final @NotNull String rest = unreadable.size() > 5
                ? Bundle.message("export.unreadable.more", String.valueOf(unreadable.size() - 5))
                : "";
        final @NotNull String count = unreadable.size() == 1
                ? Bundle.message("export.unreadable.one")
                : Bundle.message("export.unreadable.many", String.valueOf(unreadable.size()));

        return Bundle.message("export.unreadable.message", count, named, rest);
    }

    private static @NotNull String uniqueKey(final @NotNull Map<String, ?> taken, final @NotNull List<String> path) {
        for (int from = path.size() - 1; from >= 0; from--) {
            final @NotNull String key = String.join(" - ", path.subList(from, path.size()));
            if (!taken.containsKey(key)) return key;
        }

        return String.join(" - ", path) + " (" + (taken.size() + 1) + ")";
    }

    private static @NotNull Optional<VirtualFile> resolveTargetDir(final @NotNull DirectoryDto dirDto) {
        return Optional.ofNullable(LocalFileSystem.getInstance().findFileByPath(dirDto.getPath().toString()))
                .map(target -> target.isDirectory() ? target : target.getParent());
    }

    // UC-SHARE-001, UC-SHARE-002
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.firstSelected(e, DirectoryDto.class).ifPresent(dir -> new Work(p).exportFrom(dir));
    }

    // UC-SHARE-001
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TestinData.singleSelectedNode(e)
                .filter(DirectoryDto::isTestCaseContainer)
                .isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private record Work(@NotNull Project p) {
        // UC-SHARE-001, Rule-SHARE-015
        private void exportFrom(final @NotNull DirectoryDto dirDto) {
            final @NotNull Optional<VirtualFile> resolved = resolveTargetDir(dirDto);
            if (resolved.isEmpty()) return;
            final @NotNull VirtualFile targetDir = resolved.orElseThrow();

            BackgroundWork.run(p, Bundle.message("export.task.reading", dirDto.getName()), Bundle.message("export.failed.title"), gathering -> {
                final @NotNull Gathered gathered = gather(dirDto);
                final @NotNull Map<String, List<TestCaseDto>> sheets = gathered.sheets();
                if (sheets.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("notification.export.empty.title"), Bundle.message("export.none.found")));
                    return;
                }

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (gathered.unreadable().isEmpty()) {
                        chooseWhatToExport(sheets, targetDir);
                        return;
                    }

                    new ConfirmDialog(p, Bundle.message("export.unreadable.title"), unreadableWarning(gathered.unreadable()),
                            "", "", Bundle.message("export.anyway"), () -> chooseWhatToExport(sheets, targetDir)).show();
                });
            });
        }

        private void chooseWhatToExport(final @NotNull Map<String, List<TestCaseDto>> sheets, final @NotNull VirtualFile targetDir) {
            new ExportDialog(p, TestEditorAttributes.all(Can.EXPORT), sheets, targetDir, this::writeExport).show();
        }

        // UC-SHARE-001, Rule-SHARE-005
        private void writeExport(final DestinationForm.@NotNull Destination destination, final @NotNull Map<String, List<TestCaseDto>> selected) {
            final int testCases = selected.values().stream().mapToInt(List::size).sum();

            BackgroundWork.run(p, Bundle.message("export.task.writing", String.valueOf(testCases), destination.file().getName()),
                    Bundle.message("export.failed.title"), indicator -> {
                        destination.format().exportToFile(p, destination.file(), selected);

                        ExportNotice.show(p, destination.file(), testCases);
                    });
        }

        // UC-SHARE-002, Rule-SHARE-001
        private @NotNull Gathered gather(final @NotNull DirectoryDto node) {
            final @NotNull List<Sheet> found = new ArrayList<>();
            final @NotNull List<String> unreadable = new ArrayList<>();

            walk(node, List.of(node.getName()), found, unreadable);

            final @NotNull Map<String, List<TestCaseDto>> sheets = new LinkedHashMap<>();
            for (final Sheet sheet : found) sheets.put(uniqueKey(sheets, sheet.path()), sheet.testCases());

            return new Gathered(sheets, unreadable);
        }

        // UC-SHARE-003, Rule-SHARE-020
        private @NotNull List<TestCaseDto> detached(final @NotNull List<TestCaseDto> testCases) {
            return testCases.stream()
                    .map(tc -> TestCaseSnapshot.copy(p, tc))
                    .toList();
        }

        private void walk(final @NotNull DirectoryDto node, final @NotNull List<String> path, final @NotNull List<Sheet> found, final @NotNull List<String> unreadable) {
            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

            final @NotNull List<TestCaseDto> here = indexer.getTestCasesForTestSet(node.getPath());
            if (!here.isEmpty()) found.add(new Sheet(path, detached(here)));

            unreadable.addAll(indexer.unreadableTestCasesIn(node.getPath()).stream().sorted().toList());

            for (final DirectoryDto child : indexer.getChildren(node.getPath())) {
                final @NotNull List<String> under = new ArrayList<>(path);
                under.add(child.getName());
                walk(child, under, found, unreadable);
            }
        }
    }

    private record Gathered(@NotNull Map<String, List<TestCaseDto>> sheets, @NotNull List<String> unreadable) {
    }

    private record Sheet(@NotNull List<String> path, @NotNull List<TestCaseDto> testCases) {
    }
}
