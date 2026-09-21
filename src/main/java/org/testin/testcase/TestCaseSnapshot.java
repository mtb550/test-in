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

package org.testin.testcase;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.undo.UndoScope;
import org.testin.undo.UndoHistories;
import org.testin.editor.TestinEditors;
import org.testin.util.Bundle;
import org.testin.util.Mapper;
import org.testin.view.ViewToolWindowFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record TestCaseSnapshot(@NotNull Project p, @NotNull Path testSetPath, @NotNull List<TestCaseDto> present, @NotNull List<UUID> absent) {
    // UC-EDITOR-PANEL-012, Rule-EDITOR-PANEL-228
    public static @NotNull TestCaseSnapshot of(final @NotNull Project p, final @NotNull Path testSetPath, final @NotNull List<UUID> ids) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        final @NotNull List<TestCaseDto> present = new ArrayList<>(ids.size());
        final @NotNull List<UUID> absent = new ArrayList<>();

        for (final UUID id : ids)
            indexer.findTestCase(id)
                    .filter(tc -> tc.getParent().getPath().equals(testSetPath))
                    .ifPresentOrElse(tc -> present.add(copy(p, tc)), () -> absent.add(id));

        return new TestCaseSnapshot(p, testSetPath, present, absent);
    }

    // UC-EDITOR-PANEL-012, Rule-EDITOR-PANEL-067
    public static @NotNull String describe(final @NotNull String verb, final @NotNull List<TestCaseDto> cases) {
        return cases.size() == 1
                ? Bundle.message("snapshot.undo.one", verb, cases.getFirst().getDescription())
                : Bundle.message("snapshot.undo.many", verb, String.valueOf(cases.size()));
    }

    public static @NotNull List<UUID> idsOf(final @NotNull List<TestCaseDto> cases) {
        return cases.stream().map(TestCaseDto::getId).toList();
    }

    // UC-EDITOR-PANEL-012, Rule-EDITOR-PANEL-038
    public static void record(final @NotNull Project p, final @NotNull String description, final @NotNull TestCaseSnapshot before, final @NotNull TestCaseSnapshot after) {
        record(p, UndoScope.of(before.testSetPath()), description, List.of(before), List.of(after));
    }

    // UC-EDITOR-PANEL-012, Rule-EDITOR-PANEL-070
    public static void record(final @NotNull Project p, final @NotNull UndoScope scope, final @NotNull String description, final @NotNull List<TestCaseSnapshot> before, final @NotNull List<TestCaseSnapshot> after) {
        if (same(before, after)) return;

        ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(p, UndoHistories.class).push(scope, new UndoHistories.Operation(
                description,
                () -> restore(p, before, after),
                () -> restore(p, after, before),
                () -> {
                })));
    }

    // UC-INTERNAL-005, Rule-INTERNAL-063
    private static boolean restore(final @NotNull Project p, final @NotNull List<TestCaseSnapshot> target, final @NotNull List<TestCaseSnapshot> expected) {
        if (!expected.stream().allMatch(TestCaseSnapshot::stillStands)) {
            Services.getInstance(p, Notifier.class).softRefuse(p,
                    Bundle.message("snapshot.changed.title"),
                    Bundle.message("snapshot.changed.message"));
            return false;
        }

        // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-215
        final @NotNull Written written = new Written();
        final boolean allBack = ProgressManager.getInstance().<Boolean, RuntimeException>runProcessWithProgressSynchronously(() -> {
            boolean all = true;
            for (final TestCaseSnapshot snapshot : target) all &= snapshot.removeAbsent(written);
            for (final TestCaseSnapshot snapshot : target) all &= snapshot.restorePresent(written);
            return all;
        }, Bundle.message("remove.undo.progress"), false, p);

        written.generate(p);
        tellTheSurfaces(p, target);
        return allBack;
    }

    private record Written(@NotNull List<TestCaseDto> removed, @NotNull List<TestCaseDto> comingBack, @NotNull List<TestCaseDto> landed) {
        Written() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        void generate(final @NotNull Project p) {
            if (!removed.isEmpty()) GenType.REMOVE_TEST_CASE.executeAll(p, removed);
            if (!comingBack.isEmpty()) GenType.CREATE_TEST_CASE.executeAll(p, comingBack);

            // UC-CODEGEN-002, Rule-CODEGEN-068
            if (!landed.isEmpty()) GenType.RECONCILE_TEST_CASE.executeAll(p, landed);
        }
    }

    private static void tellTheSurfaces(final @NotNull Project p, final @NotNull List<TestCaseSnapshot> written) {
        final @NotNull TestinEditors editors = Services.getInstance(p, TestinEditors.class);

        written.forEach(snapshot -> editors.reloadOpen(p, snapshot.testSetPath()));

        written.forEach(snapshot -> ViewToolWindowFactory.refreshIfShowing(p, snapshot.present()));
    }

    private static boolean same(final @NotNull List<TestCaseSnapshot> before, final @NotNull List<TestCaseSnapshot> after) {
        if (before.size() != after.size()) return false;

        for (int i = 0; i < before.size(); i++)
            if (!before.get(i).sameAs(after.get(i))) return false;

        return true;
    }

    private boolean stillStands() {
        return Services.getInstance(p, ProjectIndexer.class).nodeExists(testSetPath) && sameAs(of(p, testSetPath, ids()));
    }

    public @NotNull List<UUID> ids() {
        final @NotNull List<UUID> ids = new ArrayList<>(idsOf(present));
        ids.addAll(absent);
        return ids;
    }

    private boolean sameAs(final @NotNull TestCaseSnapshot other) {
        return absent.equals(other.absent) && asJson().equals(other.asJson());
    }

    private @NotNull List<String> asJson() {
        final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);
        return present.stream().map(mapper::writeValueAsString).sorted().toList();
    }

    // UC-EDITOR-PANEL-017, Rule-EDITOR-PANEL-215
    private boolean removeAbsent(final @NotNull Written written) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        final @NotNull List<TestCaseDto> stillThere = absent.stream().flatMap(id -> indexer.findTestCase(id).stream()).toList();

        boolean allWent = true;
        for (final TestCaseDto tc : stillThere) {
            if (indexer.removeTestCase(testSetPath, tc.getId())) written.removed().add(tc);
            else allWent = false;
        }

        return allWent;
    }

    // UC-EDITOR-PANEL-017
    private boolean restorePresent(final @NotNull Written written) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        final @NotNull TestSetDirectoryDto parent = indexer.getTestSetByPath(testSetPath);
        present.forEach(tc -> tc.setParent(parent));

        boolean allBack = true;
        for (final TestCaseDto tc : present) {
            final boolean isComingBack = indexer.findTestCase(tc.getId()).isEmpty();

            final @NotNull TestCaseDto stored = copy(p, tc);
            stored.setParent(parent);
            if (!indexer.putTestCaseVerbatim(testSetPath, stored)) {
                allBack = false;
                continue;
            }

            written.landed().add(tc);
            if (isComingBack) written.comingBack().add(tc);
        }

        return allBack;
    }

    // UC-EDITOR-PANEL-012, Rule-EDITOR-PANEL-238
    public static @NotNull TestCaseDto copy(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        return Services.getInstance(p, Mapper.class).convertValue(tc, TestCaseDto.class).setParent(tc.getParent());
    }
}
