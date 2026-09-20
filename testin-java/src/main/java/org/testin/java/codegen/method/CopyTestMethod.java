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

package org.testin.java.codegen.method;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CopiedCase;
import org.testin.codegen.GenAction;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.java.codegen.method.update.UpdateTestBase;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// UC-CODEGEN-002, Rule-CODEGEN-078
public class CopyTestMethod extends UpdateTestBase implements GenAction {
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (obj instanceof CopiedCase copied) executeAll(p, List.of(copied));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-078
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        final @NotNull List<CopiedCase> copies = new ArrayList<>();
        for (final Object item : items) {
            if (item instanceof CopiedCase copied) copies.add(copied);
        }
        if (copies.isEmpty()) return;

        new CreateTestMethod().executeAll(p, copies.stream().map(CopiedCase::copy).toList());

        final @NotNull Optional<PsiClass> into = classOf(p, copies.getFirst().copy());
        if (into.isEmpty()) return;

        final @NotNull PsiClass target = into.orElseThrow();

        int carried = 0;
        for (final CopiedCase copied : copies) {
            if (carryBody(p, target, copied)) carried++;
        }

        if (carried > 0) {
            reformat(p, target);
            Logger.info("Carried " + carried + " test method body(s) into " + target.getQualifiedName());
        }
    }

    private boolean carryBody(final @NotNull Project p, final @NotNull PsiClass target, final @NotNull CopiedCase copied) {
        final @NotNull TestCaseDto original = copied.original();

        if (original.getParent().getPath().toString().isEmpty()) {
            Logger.debug("Nothing to carry into the copy of '" + original.getDescription() + "': its test set is not known");
            return false;
        }

        final @NotNull Optional<PsiClass> from = classOf(p, original);
        if (from.isEmpty()) {
            Logger.debug("Nothing to carry into the copy of '" + original.getDescription() + "': its test set has no class");
            return false;
        }

        final @NotNull Optional<PsiCodeBlock> body = GeneratedMethod.forCase(from.orElseThrow(), original)
                .map(PsiMethod::getBody);

        if (body.isEmpty()) {
            Logger.debug("Nothing to carry into the copy of '" + original.getDescription() + "': it has no method with a body");
            return false;
        }

        final @NotNull Optional<PsiCodeBlock> written = GeneratedMethod.forCase(target, copied.copy())
                .map(PsiMethod::getBody);

        if (written.isEmpty()) {
            Logger.debug("No method to fill for the copy of '" + original.getDescription() + "'");
            return false;
        }

        written.orElseThrow().replace(body.orElseThrow());
        return true;
    }
}
