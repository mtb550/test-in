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

/**
 * UC-CODEGEN-002, Rule-CODEGEN-078.
 * <p>
 * Gives a pasted copy a method of its own, with the body of the method it was
 * copied from in it.
 * <p>
 * A copy used to get the method a newly created case gets: the right annotation,
 * the right name, and a TODO where the steps should be. Everything Testin knows
 * about the case came across and the one part the tester had written did not, so
 * copying a test case to vary it - the reason there is a copy - started from an
 * empty method every time.
 * <p>
 * <b>Only the body.</b> A copy is a different test case, so its method keeps its
 * own id, its own name and its own attributes; the id in particular is what
 * Run and Navigate to Code find it by, and two methods carrying one id is the
 * defect {@link GeneratedMethod} exists to prevent.
 * <p>
 * <b>Written by the create generator, then filled.</b> What a generated method
 * looks like has one owner and this is not it - {@link CreateTestMethod} writes
 * the method here, with everything it does about names already taken, a
 * description that cannot name a method, and a name another case already
 * answers to. This adds the one thing that generator has no way to know.
 * <p>
 * <b>The body is carried as written.</b> A body calling a helper the destination
 * class does not have does not compile there, exactly as it would not had the
 * tester pasted it by hand - and the IDE says so on the line. Rewriting someone's
 * automation to fit is not something to do behind them.
 */
public class CopyTestMethod extends UpdateTestBase implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (obj instanceof CopiedCase copied) executeAll(p, List.of(copied));
    }

    /**
     * UC-CODEGEN-002, Rule-CODEGEN-078.
     * <p>
     * One paste, whatever it held: the methods are written in one go and the
     * bodies carried after, so a copy of twenty cases is one entry on the IDE's
     * undo history rather than forty.
     * <p>
     * No command of its own. {@code GenType} opened one around this call, and
     * the create generator's own is merged into it - the rule every generator in
     * this package follows (#153).
     */
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        final @NotNull List<CopiedCase> copies = new ArrayList<>();
        for (final Object item : items) {
            if (item instanceof CopiedCase copied) copies.add(copied);
        }
        if (copies.isEmpty()) return;

        new CreateTestMethod().executeAll(p, copies.stream().map(CopiedCase::copy).toList());

        // The destination is one test set, because a paste goes into one editor -
        // so the class is resolved once and reformatted once, rather than per
        // case.
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

    /**
     * The original's body onto the copy's method, and whether it went.
     * <p>
     * Every way this answers no leaves the empty method the create generator
     * just wrote, which is what a copy used to get in every case: the copy is a
     * test case either way, and a method with a TODO in it is a place for the
     * tester to write rather than a failure.
     */
    private boolean carryBody(final @NotNull Project p, final @NotNull PsiClass target, final @NotNull CopiedCase copied) {
        final @NotNull TestCaseDto original = copied.original();

        // A case whose test set is not known is the clipboard's own copy, which
        // the paste falls back to when the index no longer holds the original.
        // Asked for its class it would answer DefaultTest - the name Fqcn gives a
        // case sitting directly in the test cases directory - and a project that
        // has one would have a body carried out of somebody else's method.
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
            // The create generator says why it wrote no method for this case, in
            // the words that name what to do about it - a description that
            // cannot become a method name, or a name another case already
            // answers to. Saying it again here would be the same news twice.
            Logger.debug("No method to fill for the copy of '" + original.getDescription() + "'");
            return false;
        }

        written.orElseThrow().replace(body.orElseThrow());
        return true;
    }
}
