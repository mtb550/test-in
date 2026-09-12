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

package org.testin.java.codegen;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.PsiMethod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.openapi.project.Project;
import org.testin.model.dto.TestCaseDto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Which generated method belongs to a test case.
 * <p>
 * By the id the generator wrote into the method's {@code @Test} annotation, not
 * by the method's name. The name is the case's description with the punctuation
 * taken out, so two cases whose descriptions differ only in punctuation or in
 * capitals - "Login" and "Log-in", "Add user" and "add user" - sanitize to the
 * same name and only one method is ever written. Asked by name, both cases
 * answer with that one method; asked by id, the second answers honestly that it
 * has none.
 * <p>
 * One owner because there were two answers to this question in the plugin and
 * they disagreed. The updaters asked by id and the runner asked by name, so the
 * runner collapsed the two cases into one pattern entry, TestNG ran the method
 * once, and the report carried the first case's id - the second case ran,
 * reported nothing and ended with no verdict at all. The same gap made a method
 * a tester renamed by hand invisible to Run while the updaters kept editing it.
 * <p>
 * Reads PSI, so a caller holds the read action.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GeneratedMethod {

    /**
     * The attribute carrying the case's id, which is the only part of a
     * generated method that must never be edited.
     */
    private static final @NotNull String TEST_NAME = "testName";

    /**
     * Rule-CODEGEN-001.
     * <p>
     * The method in this class carrying this case's id, and empty when none
     * does.
     */
    public static @NotNull Optional<PsiMethod> forCase(final @NotNull PsiClass pc, final @NotNull TestCaseDto tc) {
        return Optional.ofNullable(byCaseId(pc).get(tc.getId().toString()));
    }

    /**
     * Rule-CODEGEN-001.
     * <p>
     * Every generated method in the class, by the case id it carries.
     * <p>
     * One pass for a caller with a set to place rather than one scan per case.
     * The order sweep asks for all of them - the position of every case in the
     * set, and where each method goes - so asking per case walked every method
     * in the class for every case in the set: 120 cases in one class is 14,400
     * scans of the same list for one drag.
     */
    public static @NotNull Map<String, PsiMethod> byCaseId(final @NotNull PsiClass pc) {
        final @NotNull Map<String, PsiMethod> byId = new LinkedHashMap<>();

        for (final PsiMethod pm : pc.getMethods()) {
            caseIdOf(pm).ifPresent(id -> byId.putIfAbsent(id, pm));
        }

        return byId;
    }

    /**
     * UC-CODEGEN-002, Rule-CODEGEN-013.
     * <p>
     * The case id in a method's {@code @Test}, and empty on a method that
     * carries none.
     * <p>
     * Read as the attribute rather than found in the annotation's text. It used
     * to ask whether the rendered annotation contained the word {@code testName}
     * and contained the id, which is true of an annotation that merely mentions
     * either - a description holding an id pasted into it answered for a case
     * that is not this one. It also rendered the whole annotation to a string
     * for every method of the class, for every case being asked about.
     */
    public static @NotNull Optional<String> caseIdOf(final @NotNull PsiMethod pm) {
        return testAnnotationOf(pm)
                .map(annotation -> annotation.findDeclaredAttributeValue(TEST_NAME))
                .filter(PsiLiteralValue.class::isInstance)
                .map(value -> ((PsiLiteralValue) value).getValue())
                .filter(String.class::isInstance)
                .map(String.class::cast);
    }

    
    /**
     * UC-CODEGEN-002, Rule-CODEGEN-001.
     * <p>
     * Writes this case's id into a method the tester wrote themselves, so that
     * everything which finds a method by id can find it (#66, finding 41).
     * <p>
     * <b>Why the import needs it.</b> Navigate to Code, Run, and every updater
     * find a case's method by the id in its {@code @Test(testName = ...)}, and a
     * class automated by hand carries none. So importing that class's sheet used
     * to skip every method as already there and leave every case answering "no
     * automation has been generated yet" while the code sat right beside it -
     * proved on a 150-case class where the ids had to be stamped in by an
     * external script before a single case could navigate.
     * <p>
     * Only onto a method that has no id. One that carries another case's id
     * belongs to that case, and overwriting it would take the method away from
     * whoever owns it.
     */
    public static void adopt(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull TestCaseDto tc) {
        if (caseIdOf(pm).isPresent()) return;

        testAnnotationOf(pm).ifPresent(annotation -> {
            final @NotNull PsiAnnotation written = JavaPsiFacade.getElementFactory(p)
                    .createAnnotationFromText("@Test(" + TEST_NAME + " = " + JavaLiteral.of(tc.getId().toString()) + ")", pm);

            Optional.ofNullable(written.findDeclaredAttributeValue(TEST_NAME))
                    .ifPresent(value -> annotation.setDeclaredAttributeValue(TEST_NAME, value));
        });
    }

/**
     * The method's {@code @Test} annotation, empty on a method that has none.
     */
    public static @NotNull Optional<PsiAnnotation> testAnnotationOf(final @NotNull PsiMethod pm) {
        return Optional.ofNullable(pm.getModifierList().findAnnotation("org.testng.annotations.Test"));
    }
}
