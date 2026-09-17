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
import org.testin.codegen.MovedCase;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.java.codegen.method.update.UpdateTestBase;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;

import java.util.List;
import java.util.Optional;

/**
 * UC-CODEGEN-002, Rule-CODEGEN-077.
 * <p>
 * Moves a test case's generated method into the class of the test set it was
 * pasted into, with the body the tester wrote in it.
 * <p>
 * A cut and paste used to leave the method behind. The case was in its new test
 * set and its method in the old class, so Run and Go to code looked where the
 * case now lives and found nothing - and removing the old test set later deleted
 * that class with the automation still inside it (#312, A54).
 * <p>
 * <b>The text, not a fresh method.</b> Creating one in the destination and
 * removing the old one would be two generators that already exist, and would
 * throw away the only thing worth moving: the body. So the method is carried
 * across as it is written, annotation and all, and then deleted where it was.
 * <p>
 * <b>Nothing is deleted until the copy is in.</b> A destination class that
 * cannot be found leaves the method exactly where it is: a method in the wrong
 * class is a thing a tester can find and move by hand, and one this deleted is
 * not.
 */
public class MoveTestMethod extends UpdateTestBase implements GenAction {

    /**
     * What a generated class imports to carry a {@code @Test}. Named here as
     * well as in {@link CreateTestMethod} because the destination may be a class
     * nothing has generated into yet, and a method pasted in without it does not
     * compile.
     */
    private static final @NotNull String TESTNG_TEST = "org.testng.annotations.Test";

    // UC-CODEGEN-002, Rule-CODEGEN-077
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof MovedCase moved)) return;

        final @NotNull Runnable inCommand = () ->
                WriteCommandAction.runWriteCommandAction(p, "Move Test Method", null, () -> move(p, moved));

        // Straight through when a command is already open, and only then hop -
        // the same rule every update in this package follows, so a paste of
        // twenty cases is one entry on the IDE's undo history rather than twenty.
        if (CommandProcessor.getInstance().getCurrentCommand() != null) inCommand.run();
        else ApplicationManager.getApplication().invokeLater(inCommand);
    }

    private void move(final @NotNull Project p, final @NotNull MovedCase moved) {
        final @NotNull TestCaseDto tc = moved.tc();

        final @NotNull Optional<PsiClass> from = classOf(p, Fqcn.ofClass(moved.from()));
        if (from.isEmpty()) {
            Logger.debug("Nothing to move for '" + tc.getDescription() + "': the test set it came from has no class");
            return;
        }

        final @NotNull Optional<PsiMethod> method = GeneratedMethod.forCase(from.orElseThrow(), tc);
        if (method.isEmpty()) {
            Logger.debug("Nothing to move for '" + tc.getDescription() + "': no method with testName=" + tc.getId());
            return;
        }

        final @NotNull Optional<PsiClass> into = classOf(p, Fqcn.ofClass(tc.getParent()));
        if (into.isEmpty()) {
            // Left where it is, and said out loud rather than debugged: a method
            // in the wrong class is a thing a tester can find, and one this
            // deleted is not.
            Logger.warn("Left the method for '" + tc.getDescription() + "' where it was: "
                    + "the test set it moved into has no class to put it in");
            return;
        }

        final @NotNull PsiClass target = into.orElseThrow();

        // Already there, which is what a paste into the set the case came from
        // means: the two classes are one, and moving a method to where it is
        // would delete it.
        if (GeneratedMethod.forCase(target, tc).isPresent()
                && target.equals(from.orElseThrow())) {
            Logger.debug("The method for '" + tc.getDescription() + "' is already in the right class");
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

    /**
     * The class a test set's generated code lives in, and nothing when the set
     * has none or the name does not resolve.
     */
    private @NotNull Optional<PsiClass> classOf(final @NotNull Project p, final @NotNull List<String> fqcn) {
        if (fqcn.isEmpty()) return Optional.empty();

        return Optional.ofNullable(JavaPsiFacade.getInstance(p)
                .findClass(String.join(".", fqcn), GlobalSearchScope.projectScope(p)));
    }

    /**
     * Gives the destination file the TestNG import, when it has not got one. A
     * method carried in with its annotation does not compile without it.
     */
    private void addTestImport(final @NotNull Project p, final @NotNull PsiClass target) {
        final @NotNull PsiFile file = target.getContainingFile();
        if (!(file instanceof PsiJavaFile javaFile)) return;

        final @NotNull Optional<PsiImportList> imports = Optional.ofNullable(javaFile.getImportList());
        if (imports.isEmpty() || imports.orElseThrow().findSingleClassImportStatement(TESTNG_TEST) != null) return;

        final @NotNull PsiElementFactory factory = JavaPsiFacade.getElementFactory(p);

        Optional.ofNullable(JavaPsiFacade.getInstance(p).findClass(TESTNG_TEST, GlobalSearchScope.allScope(p)))
                .ifPresent(testClass -> imports.orElseThrow().add(factory.createImportStatement(testClass)));
    }

    /**
     * Both classes are reformatted, because both changed: one gained a method
     * and the other lost one.
     */
    private void reformat(final @NotNull Project p, final @NotNull PsiClass pc) {
        CodeStyleManager.getInstance(p).reformat(pc);
    }
}
