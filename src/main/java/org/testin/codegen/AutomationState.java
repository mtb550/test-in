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

package org.testin.codegen;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.Automated;
import org.testin.model.dto.TestCaseDto;
import org.testin.navigate.CodeNavigation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service(Service.Level.PROJECT)
public final class AutomationState {
    private final @NotNull Map<UUID, Automated> known = new ConcurrentHashMap<>();

    // UC-CODEGEN-005, Rule-CODEGEN-025
    private final @NotNull Set<UUID> withAMethod = ConcurrentHashMap.newKeySet();

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-195, Rule-CODEGEN-002
    private static @NotNull Automated stateOf(final @NotNull TestCaseDto tc, final @NotNull Map<UUID, Boolean> methods) {
        final @NotNull Optional<Boolean> method = Optional.ofNullable(methods.get(tc.getId()));

        if (method.isPresent()) return method.orElseThrow() ? Automated.WRITTEN : Automated.NONE;

        return Fqcn.methodNameOf(tc).isEmpty() ? Automated.NONE : Automated.MISSING;
    }

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-197
    public @NotNull Automated of(final @NotNull UUID id) {
        return known.getOrDefault(id, Automated.UNKNOWN);
    }

    // UC-CODEGEN-005, Rule-CODEGEN-025
    public boolean hasMethod(final @NotNull UUID id) {
        return withAMethod.contains(id);
    }

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-196
    public void read(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases, final @NotNull Runnable onAnswered) {
        if (testCases.isEmpty()) return;

        // Rule-CODEGEN-082
        if (!CodeOn.isOn(p)) {
            if (known.isEmpty() && withAMethod.isEmpty()) return;

            known.clear();
            withAMethod.clear();
            ApplicationManager.getApplication().invokeLater(onAnswered);
            return;
        }

        if (DumbService.isDumb(p)) {
            DumbService.getInstance(p).runWhenSmart(() -> read(p, testCases, onAnswered));
            return;
        }

        final @NotNull Map<UUID, Automated> asking = testCases.stream()
                .collect(Collectors.toMap(TestCaseDto::getId, tc -> of(tc.getId()), (first, _) -> first));

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Map<UUID, Automated> answers = new LinkedHashMap<>();
            final @NotNull Set<UUID> found = new LinkedHashSet<>();

            ApplicationManager.getApplication().runReadAction(() -> {
                try {
                    final @NotNull Map<UUID, Boolean> methods = CodeNavigation.available().methodsFor(p, testCases);

                    for (final TestCaseDto tc : testCases) {
                        answers.put(tc.getId(), stateOf(tc, methods));
                        if (methods.containsKey(tc.getId())) found.add(tc.getId());
                    }
                } catch (final Exception ex) {
                    Logger.warn("Could not read the automation state of " + testCases.size() + " test case(s): " + ex.getMessage());
                }
            });

            if (answers.isEmpty()) return;

            ApplicationManager.getApplication().invokeLater(() -> {
                known.putAll(answers);

                for (final TestCaseDto tc : testCases) {
                    if (found.contains(tc.getId())) withAMethod.add(tc.getId());
                    else withAMethod.remove(tc.getId());
                }

                if (answers.equals(asking)) return;

                Logger.debug("Automation state read: " + answers.size() + " case(s), "
                        + answers.values().stream().filter(state -> state == Automated.WRITTEN).count() + " automated");

                onAnswered.run();
            });
        });
    }

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-210
    public int writtenIn(final @NotNull List<TestCaseDto> testCases) {
        return (int) testCases.stream().filter(tc -> of(tc.getId()) == Automated.WRITTEN).count();
    }

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-211
    public int knownIn(final @NotNull List<TestCaseDto> testCases) {
        return (int) testCases.stream().filter(tc -> of(tc.getId()) != Automated.UNKNOWN).count();
    }

    // UC-EDITOR-PANEL-047, Rule-EDITOR-PANEL-198
    public @NotNull List<TestCaseDto> matching(final @NotNull List<TestCaseDto> testCases, final @NotNull Set<Automated> wanted) {
        if (wanted.isEmpty()) return testCases;

        return testCases.stream().filter(tc -> wanted.contains(of(tc.getId()))).toList();
    }
}
