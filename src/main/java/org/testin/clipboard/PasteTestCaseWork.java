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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CopiedTestCase;
import org.testin.codegen.GenType;
import org.testin.codegen.MovedTestCase;
import org.testin.editor.TestinEditor;
import org.testin.editor.test.TestEditor;
import org.testin.indexer.TestCases;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestCaseDto.TestCaseDtoBuilder;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testcase.Rank;
import org.testin.testcase.TestCaseOrder;
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

record PasteTestCaseWork(@NotNull Project p, @NotNull TestinEditor editor, @NotNull CutState cutState, @NotNull TestCases testCases, @NotNull Notifier notifier, @NotNull Mapper mapper, @NotNull AppSettingsState settings) {
    PasteTestCaseWork(final @NotNull Project p, final @NotNull TestinEditor editor) {
        this(p, editor, Services.getInstance(p, CutState.class), Services.getInstance(p, TestCases.class), Services.getInstance(p, Notifier.class), Services.getInstance(p, Mapper.class), Services.getInstance(p, AppSettingsState.class));
    }

    void paste() {
        final @NotNull List<TestCaseDto> pastedTestCases = getFromClipboard();
        if (pastedTestCases.isEmpty()) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!(editor instanceof TestEditor destUI)) return;

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

            // Rule-EDITOR-PANEL-083
            final @NotNull List<String> ranks = ranksUnderTheSelection(destUI, isCut ? Set.copyOf(TestCaseSnapshot.idsOf(pastedTestCases)) : Set.of(), pastedTestCases.size());

            for (int i = 0; i < pastedTestCases.size(); i++) {
                final @NotNull TestCaseDto tc = pastedTestCases.get(i);
                final @NotNull TestCaseDto clonedTc = cloneForPasting(tc, isCut, destUI.getParent(), ranks.get(i));

                destUI.getAllTestCases().add(clonedTc);
                pastedHere.add(clonedTc);

                if (!isCut) {
                    copied.add(new CopiedTestCase(clonedTc,
                            testCases.findTestCase(tc.getId()).orElse(tc)));
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

                notifier.softShowCounted(p, Done.PASTED, pasted);
            });

            if (isCut) cutState.clear();
        });
    }

    // UC-EDITOR-PANEL-017, Rule-INTERNAL-035
    private void moveCut(final @NotNull TestinEditor sourceUI, final @NotNull TestEditor destUI, final @NotNull List<TestCaseDto> cutItems, final @NotNull List<TestCaseDto> pastedHere) {
        final @NotNull Path from = sourceUI.getParent().getPath();
        final @NotNull Path to = destUI.getParent().getPath();

        final @NotNull List<TestCaseDto> stayed = new ArrayList<>();
        for (final TestCaseDto moved : pastedHere) {
            if (!testCases.moveTestCase(from, to, moved)) stayed.add(moved);
        }

        pastedHere.removeAll(stayed);
        destUI.getAllTestCases().removeAll(stayed);

        final @NotNull Set<UUID> arrived = new HashSet<>(TestCaseSnapshot.idsOf(pastedHere));
        sourceUI.getAllTestCases().removeAll(cutItems.stream().filter(tc -> arrived.contains(tc.getId())).toList());
        if (sourceUI != destUI) sourceUI.reorderAndPersist();
    }

    boolean holdsTestCases(final @NotNull Transferable contents) {
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

            final @NotNull List<TestCaseDto> parsed = mapper.readValue(json, new TypeReference<>() {
            });

            return parsed.stream().filter(Objects::nonNull).toList();
        } catch (final Exception ex) {
            Logger.warn("[WARNING] Failed to parse clipboard JSON: " + ex.getMessage());
            return List.of();
        }
    }

    // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-083
    private @NotNull List<String> ranksUnderTheSelection(final @NotNull TestEditor destUI, final @NotNull Set<UUID> leaving, final int count) {
        final @NotNull List<TestCaseDto> ordered = TestCaseOrder.ordered(destUI.getAllTestCases().stream().filter(tc -> !leaving.contains(tc.getId())).toList());
        final @NotNull Optional<TestCaseDto> anchor = destUI.getSelectedTestCases().stream()
                .filter(selected -> !selected.getOrder().isEmpty())
                .reduce((_, last) -> last);

        final int under = anchor.map(selected -> TestCaseOrder.positionOf(ordered, selected)).orElse(ordered.size());

        @NotNull String previous = under > 0 && under <= ordered.size() ? ordered.get(under - 1).getOrder() : "";
        final @NotNull String upperBound = under < ordered.size() ? ordered.get(under).getOrder() : "";

        final @NotNull List<String> ranks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            previous = Rank.between(previous, upperBound);
            ranks.add(previous);
        }

        return ranks;
    }

    // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-081, Rule-EDITOR-PANEL-082
    private @NotNull TestCaseDto cloneForPasting(final @NotNull TestCaseDto original, final boolean isCut, final @NotNull TestSetDirectoryDto parent, final @NotNull String rank) {
        final @NotNull TestCaseDtoBuilder draft = original.edit().parent(parent).order(rank);

        if (isCut) {
            final @NotNull TestCaseDto moved = draft.build();
            moved.touch(settings.testerName);
            return moved;
        }

        final @NotNull ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        return draft.id(UUID.randomUUID())
                .description(Bundle.message("paste.copy.suffix", original.getDescription()))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
