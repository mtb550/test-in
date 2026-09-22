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

package org.testin.clipboard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinData;
import org.testin.codegen.CopiedTestCase;
import org.testin.codegen.GenType;
import org.testin.codegen.MovedTestCase;
import org.testin.editor.TestinEditor;
import org.testin.editor.test.TestEditor;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testcase.TestCaseSnapshot;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;
import org.testin.util.ClipboardContents;
import org.testin.util.Mapper;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PasteTestCaseNodeAction extends DumbAwareAction {
    private @NotNull Optional<Answered> answered = Optional.empty();

    private static @NotNull Optional<Work> work(final @NotNull AnActionEvent e) {
        return Optional.ofNullable(e.getProject()).flatMap(p -> TestinData.editor(e).map(editor -> new Work(p, editor)));
    }

    // UC-EDITOR-PANEL-017
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        work(e).ifPresent(Work::paste);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Rule-EDITOR-PANEL-214
        if (TestinData.editor(e).filter(editor -> !editor.getParent().isTestCaseContainer()).isPresent()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setDescription(Bundle.message("paste.case.disabled.description"));
            return;
        }

        GrayWithReason.unless(this, e, work(e).map(this::clipboardHoldsTestCases).orElse(false), Bundle.message("paste.case.nothing.description"));
    }

    private boolean clipboardHoldsTestCases(final @NotNull Work work) {
        return ClipboardContents.withFlavor(DataFlavor.stringFlavor)
                .map(contents -> answered.filter(last -> last.contents() == contents).orElseGet(() -> {
                    final @NotNull Answered now = new Answered(contents, work.holdsTestCases(contents));
                    answered = Optional.of(now);
                    return now;
                }).holdsTestCases())
                .orElse(false);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private record Answered(@NotNull Transferable contents, boolean holdsTestCases) {
    }

    private record Work(@NotNull Project p, @NotNull TestinEditor editor) {
        void paste() {
            final @NotNull List<TestCaseDto> pastedTestCases = getFromClipboard();
            if (pastedTestCases.isEmpty()) return;

            ApplicationManager.getApplication().invokeLater(() -> {
                if (!(editor instanceof TestEditor destUI)) return;

                final @NotNull CutState cutState = Services.getInstance(p, CutState.class);
                final boolean isCut = cutState.isCutOf(pastedTestCases);

                final @NotNull Optional<DirectoryDto> cutFromSet =
                        isCut ? cutState.source().map(TestinEditor::getParent) : Optional.empty();

                if (!isCut) cutState.clear();

                final @NotNull List<TestCaseDto> cutItems = cutState.source()
                        .map(sourceUI -> sourceUI.getAllTestCases().stream().filter(tc -> cutState.isPending(tc.getId())).toList())
                        .orElseGet(List::of);
                final @NotNull Optional<TestCaseSnapshot> cutFrom = cutState.source()
                        .map(sourceUI -> TestCaseSnapshot.of(p, sourceUI.getParent().getPath(), TestCaseSnapshot.idsOf(cutItems)));

                final @NotNull List<TestCaseDto> pastedHere = new ArrayList<>(pastedTestCases.size());

                // Rule-CODEGEN-078
                final @NotNull List<CopiedTestCase> copied = new ArrayList<>(pastedTestCases.size());

                for (final TestCaseDto tc : pastedTestCases) {
                    final @NotNull TestCaseDto clonedTc = cloneForPasting(tc, isCut);

                    clonedTc.setParent(destUI.getParent());
                    destUI.getAllTestCases().add(clonedTc);
                    pastedHere.add(clonedTc);

                    if (!isCut) {
                        copied.add(new CopiedTestCase(clonedTc,
                                Services.getInstance(p, ProjectIndexer.class).findTestCase(tc.getId()).orElse(tc)));
                    }
                }

                final @NotNull Path destPath = destUI.getParent().getPath();
                final @NotNull List<UUID> pastedIds = TestCaseSnapshot.idsOf(pastedHere);
                final @NotNull List<TestCaseSnapshot> before = new ArrayList<>();
                cutFrom.ifPresent(before::add);
                before.add(TestCaseSnapshot.of(p, destPath, pastedIds));

                // Rule-EDITOR-PANEL-082, Rule-INTERNAL-035
                cutState.source().ifPresent(sourceUI -> moveCut(sourceUI, destUI, cutItems, pastedHere));

                if (pastedHere.isEmpty()) return;

                final int pasted = pastedHere.size();

                destUI.reorderAndPersist(() -> {
                    final @NotNull List<TestCaseSnapshot> after = new ArrayList<>();
                    cutFrom.ifPresent(taken -> after.add(TestCaseSnapshot.of(p, taken.testSetPath(), taken.ids())));
                    after.add(TestCaseSnapshot.of(p, destPath, pastedIds));

                    TestCaseSnapshot.record(p, UndoScope.of(destPath), TestCaseSnapshot.describe(isCut ? Bundle.message("snapshot.verb.move") : Bundle.message("snapshot.verb.paste"), pastedHere), before, after);

                    // UC-EDITOR-PANEL-017, UC-CODEGEN-002, Rule-CODEGEN-078
                    if (!isCut) GenType.COPY_TEST_CASE.executeAll(p, copied);

                    else cutFromSet.ifPresent(source -> GenType.MOVE_TEST_CASE.executeAll(p,
                            pastedHere.stream().map(moved -> new MovedTestCase(moved, source)).toList()));

                    Services.getInstance(p, Notifier.class).softShowCounted(p, Done.PASTED, pasted);
                });

                if (isCut) cutState.clear();
            });
        }

        // UC-EDITOR-PANEL-017, Rule-INTERNAL-035
        private void moveCut(final @NotNull TestinEditor sourceUI, final @NotNull TestEditor destUI, final @NotNull List<TestCaseDto> cutItems, final @NotNull List<TestCaseDto> pastedHere) {
            final @NotNull Path from = sourceUI.getParent().getPath();
            final @NotNull Path to = destUI.getParent().getPath();
            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

            final @NotNull List<TestCaseDto> stayed = new ArrayList<>();
            ApplicationManager.getApplication().runWriteAction(() -> {
                for (final TestCaseDto moved : pastedHere) {
                    if (!indexer.moveTestCase(from, to, moved)) stayed.add(moved);
                }
            });

            pastedHere.removeAll(stayed);
            destUI.getAllTestCases().removeAll(stayed);

            final @NotNull Set<UUID> arrived = new HashSet<>(TestCaseSnapshot.idsOf(pastedHere));
            sourceUI.getAllTestCases().removeAll(cutItems.stream().filter(tc -> arrived.contains(tc.getId())).toList());
            if (sourceUI != destUI && sourceUI instanceof TestEditor sourceEditor) sourceEditor.reorderAndPersist();
        }

        private boolean holdsTestCases(final @NotNull Transferable contents) {
            return !readTestCases(contents).isEmpty();
        }

        private @NotNull List<TestCaseDto> getFromClipboard() {
            return ClipboardContents.withFlavor(DataFlavor.stringFlavor)
                    .map(this::readTestCases)
                    .orElseGet(List::of);
        }

        private @NotNull List<TestCaseDto> readTestCases(final @NotNull Transferable contents) {
            try {
                final @NotNull String json = (String) contents.getTransferData(DataFlavor.stringFlavor);
                if (!json.trim().startsWith("[")) return List.of();

                final @NotNull List<TestCaseDto> parsed = Services.getInstance(p, Mapper.class).readValue(json, new TypeReference<>() {
                });

                return parsed.stream().filter(Objects::nonNull).toList();
            } catch (final Exception ex) {
                Logger.warn("[WARNING] Failed to parse clipboard JSON: " + ex.getMessage());
                return List.of();
            }
        }

        private @NotNull TestCaseDto cloneForPasting(final @NotNull TestCaseDto original, final boolean isCut) {
            final @NotNull ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

            final @NotNull TestCaseDto clonedTc = TestCaseSnapshot.copy(p, original);

            if (isCut) {
                clonedTc.touch(Services.getInstance(p, AppSettingsState.class).testerName);
            } else {
                clonedTc.setId(UUID.randomUUID())
                        .setDescription(Bundle.message("paste.copy.suffix", original.getDescription()))
                        .setCreatedAt(now)
                        .setUpdatedAt(now);
            }

            return clonedTc;
        }
    }
}
