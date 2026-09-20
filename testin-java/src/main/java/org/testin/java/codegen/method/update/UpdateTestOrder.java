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

import org.testin.codegen.GenType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.codegen.ExecutionPosition;
import org.testin.codegen.GenAction;
import org.testin.codegen.Fqcn;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Rule-CODEGEN-014
public class UpdateTestOrder extends UpdateTestBase implements GenAction {
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (obj instanceof TestCaseDto tc) executeAll(p, List.of(tc));
    }

    // UC-CODEGEN-011, Rule-CODEGEN-042, Rule-CODEGEN-067
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        final @NotNull Map<Path, List<TestCaseDto>> sets = new LinkedHashMap<>();

        for (final Object item : items) {
            if (item instanceof TestCaseDto tc) sets.computeIfAbsent(tc.getParent().getPath(), path -> ExecutionPosition.setOf(p, tc));
        }
        if (sets.isEmpty()) return;

        final @NotNull List<List<TestCaseDto>> ordered = new ArrayList<>(sets.values());

        final @NotNull Runnable inCommand = () ->
                WriteCommandAction.runWriteCommandAction(p, GenType.UPDATE_TEST_CASE_ORDER.getDescription(), null,
                        () -> ordered.forEach(inSet -> arrange(p, inSet)));

        if (CommandProcessor.getInstance().getCurrentCommand() != null) inCommand.run();
        else ApplicationManager.getApplication().invokeLater(inCommand);
    }

    // UC-CODEGEN-011, Rule-CODEGEN-043, Rule-CODEGEN-067, Rule-CODEGEN-044
    private void arrange(final @NotNull Project p, final @NotNull List<TestCaseDto> inSet) {
        if (inSet.isEmpty()) return;

        final @NotNull Optional<PsiClass> target = classOf(p, inSet.getFirst());
        if (target.isEmpty()) return;

        final @NotNull PsiClass pc = target.get();

        final @NotNull Map<String, PsiMethod> methods = GeneratedMethod.byCaseId(pc);

        @Nullable PsiElement after = null;
        int position = 0;
        int written = 0;

        for (final TestCaseDto tc : inSet) {
            position++;

            final @Nullable PsiMethod pm = methods.get(tc.getId().toString());
            if (pm == null) continue;

            updateTestAnnotationAttribute(p, pm, "priority", String.valueOf(position));
            after = place(pc, pm, after);
            written++;
        }

        if (written > 0) reformat(p, pc);
    }

    // UC-CODEGEN-011, Rule-CODEGEN-067
    private static @Nullable PsiElement place(final @NotNull PsiClass pc, final @NotNull PsiMethod pm, final @Nullable PsiElement after) {
        if (after == null) {
            final @Nullable PsiMethod first = firstGenerated(pc);

            if (first == null || first == pm) return pm;

            final @NotNull PsiElement moved = pc.addBefore(pm, first);
            pm.delete();
            return moved;
        }

        if (nextAfter(after) == pm) return pm;

        final @NotNull PsiElement moved = pc.addAfter(pm, after);
        pm.delete();
        return moved;
    }

    private static @Nullable PsiMethod firstGenerated(final @NotNull PsiClass pc) {
        for (final PsiMethod pm : pc.getMethods()) {
            if (GeneratedMethod.caseIdOf(pm).isPresent()) return pm;
        }

        return null;
    }

    private static @Nullable PsiElement nextAfter(final @NotNull PsiElement element) {
        PsiElement next = element.getNextSibling();
        while (next instanceof PsiWhiteSpace) next = next.getNextSibling();

        return next;
    }
}
