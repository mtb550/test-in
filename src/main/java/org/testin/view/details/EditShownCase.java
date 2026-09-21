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

package org.testin.view.details;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.testcase.TestCaseSnapshot;
import org.testin.testcase.create.TestCaseUpdateMenuDialog;
import org.testin.util.Bundle;
import org.testin.view.ViewToolWindowFactory;
import org.testin.codegen.GenType;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EditShownCase {
    private static final @NotNull String SHORTCUT_REGISTERED_KEY = "DetailsTab.f2.registered";

    // UC-VIEW-PANEL-011, Rule-VIEW-PANEL-044
    public static void bindTo(final @NotNull Project p, final @NotNull JBPanel<?> detailsTab) {
        if (Boolean.TRUE.equals(detailsTab.getClientProperty(SHORTCUT_REGISTERED_KEY))) return;

        detailsTab.putClientProperty(SHORTCUT_REGISTERED_KEY, Boolean.TRUE);

        new DumbAwareAction() {
            // UC-VIEW-PANEL-011, Rule-VIEW-PANEL-044
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                ViewToolWindowFactory.panel(p).ifPresent(viewPanel -> viewPanel.getCurrentTestCase()
                        .ifPresent(currentDto -> open(p, currentDto, viewPanel.getPage().getCurrentPath())));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        }.registerCustomShortcutSet(Declared.shortcutSet("Testin.UpdateTestCase"), detailsTab);
    }

    // UC-VIEW-PANEL-011, Rule-VIEW-PANEL-007
    private static void open(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull List<String> currentPath) {
        final @NotNull List<TestCaseDto> items = List.of(dto);

        final @NotNull List<UUID> ids = TestCaseSnapshot.idsOf(items);
        final @NotNull Optional<Path> undoPath = writesTo(p, dto, currentPath);
        final @NotNull Optional<TestCaseSnapshot> before = undoPath.map(editPath -> TestCaseSnapshot.of(p, editPath, ids));

        new TestCaseUpdateMenuDialog(p, items, (tcs, gt) -> save(p, dto, currentPath, tcs, gt, ids, before)).show();
    }

    // UC-VIEW-PANEL-011, Rule-VIEW-PANEL-007
    private static void save(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull List<String> currentPath, final @NotNull List<TestCaseDto> tcs, final @NotNull GenType gt, final @NotNull List<UUID> ids, final @NotNull Optional<TestCaseSnapshot> before) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        writesTo(p, dto, currentPath).ifPresentOrElse(editPath -> {
            boolean changed = false;
            for (final TestCaseDto tc : tcs) changed |= indexer.putTestCase(editPath, tc);

            if (!changed) return;

            before.ifPresent(taken -> TestCaseSnapshot.record(p, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.update"), tcs), taken, TestCaseSnapshot.of(p, editPath, ids)));

            Services.getInstance(p, Notifier.class).softShow(p, Done.UPDATED);

            ApplicationManager.getApplication().invokeLater(() -> TestCaseUpdateMenuDialog.applyAftermath(p, tcs, gt));
        }, () -> nowhereToWrite(p, dto));
    }

    private static void nowhereToWrite(final @NotNull Project p, final @NotNull TestCaseDto dto) {
        Logger.warn("No test set to write '" + dto.getDescription() + "' to - the edit was not saved");

        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("details.not.saved.title"),
                Bundle.message("details.not.saved.message"));
    }

    private static @NotNull Optional<Path> writesTo(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull List<String> currentPath) {
        final @NotNull DirectoryDto parent = dto.getParent();
        if (!parent.getPath().toString().isEmpty()) return Optional.of(parent.getPath());

        if (currentPath.isEmpty()) return Optional.empty();

        final @NotNull Path resolved = Services.getInstance(p, TestinRoot.class).resolve(currentPath);

        return Services.getInstance(p, ProjectIndexer.class).find(resolved)
                .filter(TestSetDirectoryDto.class::isInstance)
                .map(DirectoryDto::getPath);
    }
}
