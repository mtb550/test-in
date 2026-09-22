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

package org.testin.testng;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfigurationType;
import com.theoryinpractice.testng.model.TestType;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.navigate.CodeNavigation;
import org.testin.runner.TestNGExecution;
import org.testin.runner.TestRunner;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public final class TestNGRunner implements TestRunner {
    private static @NotNull Optional<Module> moduleOf(final @NotNull Project p, final @NotNull List<String> fqcn) {
        final @NotNull String classFqcn = String.join(".", fqcn.subList(0, fqcn.size() - 1));

        return Optional.ofNullable(JavaPsiFacade.getInstance(p).findClass(classFqcn, GlobalSearchScope.projectScope(p)))
                .map(ModuleUtilCore::findModuleForPsiElement);
    }

    private static @NotNull String configNameFor(final @NotNull List<Generated> generated) {
        final @NotNull List<String> classes = generated.stream().map(Generated::simpleClassName).distinct().toList();

        if (generated.size() == 1) return classes.getFirst() + "." + generated.getFirst().fqcn().getLast();
        if (classes.size() == 1) return classes.getFirst();

        return Bundle.message("codegen.named.and.more", classes.getFirst(), String.valueOf(classes.size() - 1));
    }

    @Override
    public void run(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        if (testCases.isEmpty()) return;

        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        if (DumbService.isDumb(p)) {
            testCases.forEach(execution::notStarting);

            DumbService.getInstance(p).showDumbModeNotification(Bundle.message("testng.indexing.wait"));
            return;
        }

        testCases.forEach(execution::starting);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ApplicationManager.getApplication().runReadAction(() -> prepare(p, testCases));

            } catch (final IndexNotReadyException ex) {
                testCases.forEach(execution::notStarting);

                ApplicationManager.getApplication().invokeLater(() -> DumbService.getInstance(p)
                        .showDumbModeNotification(Bundle.message("testng.indexing.interrupted")));
            }
        });
    }

    private void prepare(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        final @NotNull List<Generated> found = new ArrayList<>();
        final @NotNull List<TestCaseDto> withoutCode = new ArrayList<>();
        Optional<Module> module = Optional.empty();

        for (final TestCaseDto tc : testCases) {
            final @NotNull Optional<List<String>> method = CodeNavigation.available().methodOf(p, tc);

            if (method.isEmpty()) {
                execution.noGeneratedCode(tc);
                withoutCode.add(tc);
                continue;
            }

            found.add(new Generated(tc, method.orElseThrow()));
            if (module.isEmpty()) module = moduleOf(p, method.orElseThrow());
        }

        execution.started(List.of(), withoutCode);

        if (found.isEmpty()) return;

        final @NotNull Optional<Module> runModule = module;
        ApplicationManager.getApplication().invokeLater(() -> launch(p, found, runModule));
    }

    private void launch(final @NotNull Project p, final @NotNull List<Generated> found, final @NotNull Optional<Module> module) {
        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        final @NotNull List<TestCaseDto> stillWanted = execution.stillWanted(found.stream().map(Generated::tc).toList());
        if (stillWanted.isEmpty()) {
            Logger.info("Not starting: every case in the run was stopped before it began");
            return;
        }

        final @NotNull List<Generated> generated = found.stream().filter(one -> stillWanted.contains(one.tc())).toList();
        final @NotNull List<TestCaseDto> testCases = generated.stream().map(Generated::tc).toList();

        final @NotNull LinkedHashSet<String> patterns = new LinkedHashSet<>(generated.stream().map(Generated::pattern).toList());
        final @NotNull String name = execution.freeRunName(configNameFor(generated));

        final @NotNull RunManager runManager = RunManager.getInstance(p);
        final @NotNull TestNGConfigurationType configType = TestNGConfigurationType.getInstance();

        final @NotNull RunnerAndConfigurationSettings settings = Optional.ofNullable(runManager.findConfigurationByName(name))
                .filter(existing -> existing.getConfiguration() instanceof TestNGConfiguration)
                .orElseGet(() -> {
                    final @NotNull RunnerAndConfigurationSettings created =
                            runManager.createConfiguration(name, configType.getConfigurationFactories()[0]);
                    runManager.addConfiguration(created);
                    return created;
                });

        if (!(settings.getConfiguration() instanceof TestNGConfiguration configuration)) {
            Logger.warn("'" + name + "' is not a TestNG configuration, so the run was not started");
            return;
        }

        configuration.getPersistantData().TEST_OBJECT = TestType.PATTERN.getType();
        configuration.getPersistantData().setPatterns(patterns);
        configuration.setAllowRunningInParallel(true);

        module.ifPresent(configuration::setModule);

        runManager.setTemporaryConfiguration(settings);
        runManager.setSelectedConfiguration(settings);

        Logger.info("Running as '" + name + "': " + patterns);

        // Rule-CODEGEN-033
        execution.started(testCases, List.of());
        execution.launch(testCases, settings);
    }

    private record Generated(@NotNull TestCaseDto tc, @NotNull List<String> fqcn) {
        private @NotNull String pattern() {
            return String.join(".", fqcn.subList(0, fqcn.size() - 1)) + "," + fqcn.getLast();
        }

        private @NotNull String simpleClassName() {
            return fqcn.get(fqcn.size() - 2);
        }
    }
}
