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
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.testcase.TestCaseSnapshot;
import org.testin.testcase.create.TestCaseUpdateMenuDialog;
import org.testin.util.Bundle;
import org.testin.view.ViewToolWindowFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Editing the test case the Details panel is showing: the key that opens the
 * update menu, and writing back what it returns.
 * <p>
 * {@link DetailsTab} draws what a case <i>is</i> - its rows, its run item, its
 * badges - and carried this as well until #302. Say that class's job in a
 * sentence and then these methods', and the second is not the first: drawing a
 * case is not opening the editor for it. It was 85 lines of 322 and the only
 * part that reached a dialog, took a snapshot or wrote to disk.
 * <p>
 * <b>Where it writes is the whole difficulty.</b> A case shown from a search
 * result has no parent and no path, so there is no test set to write into - and
 * an edit that reaches no disk is one the tester believes they made, and finds
 * out about at the next open with no idea which change went (#234). Every
 * answer here runs inside the branch that found somewhere to write.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EditShownCase {

    /**
     * Set on the panel once the key is bound to it, because the panel is
     * redrawn on every selection and binding again would stack one action per
     * case the tester looked at.
     */
    private static final @NotNull String SHORTCUT_REGISTERED_KEY = "DetailsTab.f2.registered";

    /**
     * UC-VIEW-PANEL-011, Rule-VIEW-PANEL-044.
     * <p>
     * Puts the update key on the Details panel, once.
     * <p>
     * Whatever Update Test Case is bound to, not a key of this panel's own: one
     * key, one owner, and a tester who rebinds F2 rebinds it here too (#119).
     * The action itself is this panel's, because what it edits is the case the
     * panel is showing rather than an editor's selection.
     */
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
                // BGT on purpose - no update() here reads Swing state; do not switch to EDT (#52).
                return ActionUpdateThread.BGT;
            }
        }.registerCustomShortcutSet(Declared.shortcutSet("Testin.UpdateTestCase"), detailsTab);
    }

    // UC-VIEW-PANEL-011, Rule-VIEW-PANEL-007
    private static void open(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull List<String> currentPath) {
        final @NotNull List<TestCaseDto> items = List.of(dto);

        // Before the menu, for the same reason the editor's own update takes it
        // there: the dialog edits the DTO it was given.
        final @NotNull List<UUID> ids = TestCaseSnapshot.idsOf(items);
        final @NotNull Optional<Path> undoPath = writesTo(p, dto, currentPath);
        final @NotNull Optional<TestCaseSnapshot> before = undoPath.map(editPath -> TestCaseSnapshot.of(p, editPath, ids));

        new TestCaseUpdateMenuDialog(p, items, (tcs, gt) -> save(p, dto, currentPath, tcs, gt, ids, before)).show();
    }

    /**
     * UC-VIEW-PANEL-011, Rule-VIEW-PANEL-007.
     * <p>
     * What the dialog came back with, written where the case lives.
     * <p>
     * Both the confirmation and the code generation are inside the branch that
     * wrote something. They used to fire whatever happened, and there is a case
     * where nothing happens by design: a test case opened from a search result
     * has no path and no parent, so there is nowhere to write and not a byte
     * reaches disk. The tester was told "Updated" and closed the dialog on an
     * edit that was never saved.
     */
    private static void save(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull List<String> currentPath, final @NotNull List<TestCaseDto> tcs, final @NotNull org.testin.codegen.GenType gt, final @NotNull List<UUID> ids, final @NotNull Optional<TestCaseSnapshot> before) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        writesTo(p, dto, currentPath).ifPresentOrElse(editPath -> {
            boolean changed = false;
            for (final TestCaseDto tc : tcs) changed |= indexer.putTestCase(editPath, tc);

            // Nothing written, so nothing to confirm - the same reason the
            // branch above exists, one step further in: a save that reached
            // disk and changed nothing is as little of an update as one that
            // never got there (#164).
            if (!changed) return;

            before.ifPresent(taken -> TestCaseSnapshot.record(p, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.update"), tcs), taken, TestCaseSnapshot.of(p, editPath, ids)));

            Services.getInstance(p, Notifier.class).softShow(p, Done.UPDATED);

            ApplicationManager.getApplication().invokeLater(() -> TestCaseUpdateMenuDialog.applyAftermath(p, tcs, gt));
        }, () -> nowhereToWrite(p, dto));
    }

    /**
     * Said once, where it happened, and said to the tester as well as to the
     * log. An edit that reaches no disk and no screen is one the tester believes
     * they made - and they find out at the next open, with no idea which change
     * went (#234).
     */
    private static void nowhereToWrite(final @NotNull Project p, final @NotNull TestCaseDto dto) {
        Logger.warn("No test set to write '" + dto.getDescription() + "' to - the edit was not saved");

        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("details.not.saved.title"),
                Bundle.message("details.not.saved.message"));
    }

    /**
     * Where an update writes: the case's own parent when it has one, otherwise
     * the test set the navigation path names. Empty when neither says - a case
     * shown from a search result, with no path and no parent read yet.
     */
    private static @NotNull Optional<Path> writesTo(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull List<String> currentPath) {
        final @NotNull DirectoryDto parent = dto.getParent();
        if (!parent.getPath().toString().isEmpty()) return Optional.of(parent.getPath());

        if (currentPath.isEmpty()) return Optional.empty();

        final @NotNull Path resolved = Services.getInstance(p, TestinRoot.class).resolve(currentPath);

        return Optional.of(Services.getInstance(p, ProjectIndexer.class).getTestSetByPath(resolved).getPath());
    }
}
