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

package org.testin.java.codegen.method.update;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.codegen.GenAction;
import org.testin.codegen.GenType;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.model.dto.TestCaseDto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Brings a test case's generated method back into line with the case, in every
 * part of it that Testin writes.
 * <p>
 * This exists for CTRL+Z. An undo restores the case as it was and puts nothing
 * into the code, so undoing a rename left the method under the new name,
 * undoing a group change left the groups it had been given, and undoing a drag
 * left the run executing in the order the tester had just taken back. The data
 * and the code disagreed, and only the data had been asked to change (#66,
 * finding 44).
 * <p>
 * Every part at once rather than a generator per field, because a restore does
 * not know which field it is restoring - a snapshot is the case as it was, not
 * a list of what changed. Writing all of them costs one pass and is right
 * whichever field moved.
 * <p>
 * <b>Silent where there is no method.</b> A restore sweeps whatever the
 * snapshot held, which for an undone reorder is every case in the test set, and
 * a set half automated would otherwise say so once per case
 * (Rule-CODEGEN-044). Nothing is created either: a method the tester deleted on
 * purpose stays deleted, because an undo of a field edit is not a request to
 * write code that was never there.
 */
public class ReconcileTestMethod extends UpdateTestBase implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (obj instanceof TestCaseDto tc) executeAll(p, List.of(tc));
    }

    /**
     * UC-CODEGEN-002, Rule-CODEGEN-068.
     * <p>
     * One command for the whole restore, and one class lookup per class in it -
     * the same shape the order sweep takes, and for the same reason: a case at a
     * time is a write command and an undo entry each, so taking back one drag
     * cost one CTRL+Z per case (#51, and 79f99348).
     * <p>
     * The order goes last and through its own generator, because it is the one
     * part that is about the set rather than the case: it reads each case's
     * position from the index and moves the methods to match.
     */
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        final @NotNull List<TestCaseDto> cases = new ArrayList<>();
        for (final Object item : items) {
            if (item instanceof TestCaseDto tc) cases.add(tc);
        }
        if (cases.isEmpty()) return;

        final @NotNull Map<Path, List<TestCaseDto>> byClass = new LinkedHashMap<>();
        for (final TestCaseDto tc : cases) {
            byClass.computeIfAbsent(tc.getParent().getPath(), path -> new ArrayList<>()).add(tc);
        }

        ApplicationManager.getApplication().invokeLater(() ->
                WriteCommandAction.runWriteCommandAction(p, "Restore Test Case Code", null,
                        () -> byClass.values().forEach(inClass -> rewrite(p, inClass))));

        GenType.UPDATE_TEST_CASE_ORDER.getAction().executeAll(p, cases);
    }

    /**
     * The cases of one class: everything Testin writes about each of them,
     * except where it sits, which the order sweep owns.
     */
    private void rewrite(final @NotNull Project p, final @NotNull List<TestCaseDto> inClass) {
        final @NotNull Optional<PsiClass> target = classOf(p, inClass.getFirst());
        if (target.isEmpty()) return;

        final @NotNull PsiClass pc = target.orElseThrow();
        final @NotNull Map<String, PsiMethod> methods = GeneratedMethod.byCaseId(pc);

        int written = 0;
        for (final TestCaseDto tc : inClass) {
            final @Nullable PsiMethod pm = methods.get(tc.getId().toString());
            if (pm == null) continue;

            writeDescription(p, pm, tc);
            writeGroups(p, pm, tc);
            writeEnabled(p, pm, tc);
            written++;
        }

        // Once for the class, not three times per case. Each of the three
        // writes above reformatted the method it had just touched until #66's
        // finding 55, so reconciling a set of forty was a hundred and twenty
        // passes where one does (#66, finding 55).
        if (written > 0) reformat(p, pc);
    }
}
