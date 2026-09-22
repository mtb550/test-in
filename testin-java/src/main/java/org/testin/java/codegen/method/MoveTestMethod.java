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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.GenType;
import org.testin.codegen.MovedTestCase;
import org.testin.java.codegen.GeneratedClass;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.java.codegen.method.update.UpdateTestBase;
import org.testin.java.codegen.method.update.UpdateTestOrder;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// UC-CODEGEN-002, Rule-CODEGEN-077
public class MoveTestMethod extends UpdateTestBase implements GenAction {
    private static final @NotNull String TESTNG_TEST = "org.testng.annotations.Test";

    // UC-CODEGEN-002, Rule-CODEGEN-077
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        executeAll(p, List.of(obj));
    }

    // UC-CODEGEN-002, Rule-CODEGEN-077, Rule-CODEGEN-014
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        final @NotNull List<MovedTestCase> moves = new ArrayList<>();
        for (final Object item : items) {
            if (item instanceof MovedTestCase moved) moves.add(moved);
        }
        if (moves.isEmpty()) return;

        final @NotNull Runnable inCommand = () ->
                WriteCommandAction.runWriteCommandAction(p, GenType.MOVE_TEST_CASE.getDescription(), null, () -> {
                    moves.forEach(moved -> move(p, moved));
                    new UpdateTestOrder().executeAll(p, moves.stream().map(MovedTestCase::tc).toList());
                });

        if (CommandProcessor.getInstance().getCurrentCommand() != null) inCommand.run();
        else ApplicationManager.getApplication().invokeLater(inCommand);
    }

    private void move(final @NotNull Project p, final @NotNull MovedTestCase moved) {
        final @NotNull TestCaseDto tc = moved.tc();

        final @NotNull Optional<PsiClass> from = GeneratedClass.find(p, Fqcn.ofClass(moved.from()));
        if (from.isEmpty()) {
            Logger.debug("Nothing to move for '" + tc.getDescription() + "': the test set it came from has no class");
            return;
        }

        final @NotNull Optional<PsiMethod> method = GeneratedMethod.forTestCase(from.orElseThrow(), tc);
        if (method.isEmpty()) {
            Logger.debug("Nothing to move for '" + tc.getDescription() + "': no method with testName=" + tc.getId());
            return;
        }

        final @NotNull List<String> destination = Fqcn.ofClass(tc.getParent());

        final @NotNull Optional<PsiClass> into = destination.isEmpty()
                ? Optional.empty()
                : GeneratedClass.findOrWrite(p, destination.subList(0, destination.size() - 1), destination.getLast());

        if (into.isEmpty()) {
            Logger.warn("Left the method for '" + tc.getDescription() + "' where it was: "
                    + "the test set it moved into has no class to put it in");
            return;
        }

        final @NotNull PsiClass target = into.orElseThrow();

        if (target.equals(from.orElseThrow())) {
            Logger.debug("The method for '" + tc.getDescription() + "' is already in the right class");
            return;
        }

        if (GeneratedMethod.forTestCase(target, tc).isPresent()) {
            Logger.warn("Left the method for '" + tc.getDescription() + "' in " + from.orElseThrow().getQualifiedName()
                    + ": " + target.getQualifiedName() + " already has a method for this test case");
            return;
        }

        addTestImport(p, target);

        final @NotNull PsiMethod carried = JavaPsiFacade.getElementFactory(p)
                .createMethodFromText(method.orElseThrow().getText(), target);

        target.add(carried);
        method.orElseThrow().delete();

        reformat(p, target);
        reformat(p, from.orElseThrow());

        Logger.info("Moved test method " + carried.getName() + " into " + target.getName());
    }

    private void addTestImport(final @NotNull Project p, final @NotNull PsiClass target) {
        final @NotNull PsiFile file = target.getContainingFile();
        if (!(file instanceof PsiJavaFile javaFile)) return;

        final @NotNull Optional<PsiImportList> imports = Optional.ofNullable(javaFile.getImportList());
        if (imports.isEmpty() || imports.orElseThrow().findSingleClassImportStatement(TESTNG_TEST) != null) return;

        final @NotNull PsiElementFactory factory = JavaPsiFacade.getElementFactory(p);

        Optional.ofNullable(JavaPsiFacade.getInstance(p).findClass(TESTNG_TEST, GlobalSearchScope.allScope(p)))
                .ifPresent(testClass -> imports.orElseThrow().add(factory.createImportStatement(testClass)));
    }

    private void reformat(final @NotNull Project p, final @NotNull PsiClass pc) {
        CodeStyleManager.getInstance(p).reformat(pc);
    }
}
