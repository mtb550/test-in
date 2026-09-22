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

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.PsiMethod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GeneratedMethod {
    private static final @NotNull String TEST_NAME = "testName";

    // Rule-CODEGEN-001
    public static @NotNull Optional<PsiMethod> forCase(final @NotNull PsiClass pc, final @NotNull TestCaseDto tc) {
        return Optional.ofNullable(byCaseId(pc).get(tc.getId().toString()));
    }

    // Rule-CODEGEN-001
    public static @NotNull Map<String, PsiMethod> byCaseId(final @NotNull PsiClass pc) {
        final @NotNull Map<String, PsiMethod> byId = new LinkedHashMap<>();

        for (final PsiMethod pm : pc.getMethods()) {
            caseIdOf(pm).ifPresent(id -> byId.putIfAbsent(id, pm));
        }

        return byId;
    }

    // UC-CODEGEN-002, Rule-CODEGEN-013
    public static @NotNull Optional<String> caseIdOf(final @NotNull PsiMethod pm) {
        return testAnnotationOf(pm)
                .map(annotation -> annotation.findDeclaredAttributeValue(TEST_NAME))
                .filter(PsiLiteralValue.class::isInstance)
                .map(value -> ((PsiLiteralValue) value).getValue())
                .filter(String.class::isInstance)
                .map(String.class::cast);
    }

    // UC-CODEGEN-002, Rule-CODEGEN-001
    public static void adopt(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull TestCaseDto tc) {
        if (caseIdOf(pm).isPresent()) return;

        testAnnotationOf(pm).ifPresent(annotation -> {
            final @NotNull PsiAnnotation written = JavaPsiFacade.getElementFactory(p)
                    .createAnnotationFromText("@Test(" + TEST_NAME + " = " + JavaLiteral.of(tc.getId().toString()) + ")", pm);

            Optional.ofNullable(written.findDeclaredAttributeValue(TEST_NAME))
                    .ifPresent(value -> annotation.setDeclaredAttributeValue(TEST_NAME, value));
        });
    }

    public static @NotNull Optional<PsiAnnotation> testAnnotationOf(final @NotNull PsiMethod pm) {
        return Optional.ofNullable(pm.getModifierList().findAnnotation("org.testng.annotations.Test"));
    }
}
