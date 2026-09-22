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

package org.testin.java.navigate;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.java.codegen.GeneratedClass;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.navigate.CodeNavigation;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CodeNavigator implements CodeNavigation {
    private static boolean doesSomething(final @NotNull PsiMethod pm) {
        return Optional.ofNullable(pm.getBody())
                .filter(body -> body.getStatements().length > 0)
                .isPresent();
    }

    // UC-CODEGEN-006, Rule-CODEGEN-026
    private @NotNull Optional<PsiMethod> resolve(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        final @NotNull String classFqcn = Fqcn.classOfMethod(tc);
        if (classFqcn.isEmpty()) return Optional.empty();

        final @NotNull Optional<PsiClass> owner = GeneratedClass.byName(p, classFqcn);

        if (owner.isEmpty()) {
            Logger.warn("No generated class " + classFqcn + " for '" + tc.getDescription() + "'");
            return Optional.empty();
        }

        final @NotNull Optional<PsiMethod> method = GeneratedMethod.forTestCase(owner.orElseThrow(), tc);
        if (method.isEmpty()) Logger.warn("No generated method for '" + tc.getDescription() + "' in " + classFqcn);

        return method;
    }

    // UC-CODEGEN-006, Rule-CODEGEN-026
    @Override
    public @NotNull Map<UUID, Boolean> methodsFor(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        final @NotNull Map<String, List<TestCaseDto>> byClass = new LinkedHashMap<>();

        for (final TestCaseDto tc : testCases) {
            final @NotNull String classFqcn = Fqcn.classOfMethod(tc);
            if (classFqcn.isEmpty()) continue;

            byClass.computeIfAbsent(classFqcn, ignored -> new ArrayList<>()).add(tc);
        }

        final @NotNull Map<UUID, Boolean> found = new LinkedHashMap<>();

        for (final Map.Entry<String, List<TestCaseDto>> group : byClass.entrySet()) {
            final @NotNull Optional<PsiClass> owner = GeneratedClass.byName(p, group.getKey());

            if (owner.isEmpty()) continue;

            final @NotNull Map<String, PsiMethod> methods = GeneratedMethod.byTestCaseId(owner.orElseThrow());

            for (final TestCaseDto tc : group.getValue()) {
                Optional.ofNullable(methods.get(tc.getId().toString()))
                        .ifPresent(pm -> found.put(tc.getId(), doesSomething(pm)));
            }
        }

        return found;
    }

    // UC-CODEGEN-008, Rule-CODEGEN-032
    @Override
    public @NotNull Optional<List<String>> methodOf(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        return resolve(p, tc).map(method -> {
            final @NotNull List<String> named = new ArrayList<>(Fqcn.ofMethod(tc));
            named.set(named.size() - 1, method.getName());

            return named;
        });
    }

    // UC-CODEGEN-006, Rule-CODEGEN-026
    @Override
    public void toCode(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        Logger.trace("navigate to the method of '" + tc.getDescription() + "'");

        if (DumbService.isDumb(p)) {
            Logger.trace("dumb mode detected, deferring navigation");
            DumbService.getInstance(p).runWhenSmart(() -> toCode(p, tc));
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {
                    try {
                        final @NotNull Optional<PsiMethod> found = resolve(p, tc);

                        if (found.isEmpty()) {
                            ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(p, Notifier.class)
                                    .softRefuse(p, Refused.NO_GENERATED_CODE, tc.getDescription()));
                            return;
                        }

                        final @NotNull Navigatable target = found.orElseThrow();
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (target.canNavigate()) target.navigate(true);
                        });

                    } catch (final IndexNotReadyException ex) {
                        Logger.trace("index not ready, deferring navigation");
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).softShow(p, Bundle.message("navigate.waiting.for.indexing")));
                        DumbService.getInstance(p).runWhenSmart(() -> toCode(p, tc));
                    }
                })
        );
    }
}
