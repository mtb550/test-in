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

package org.testin.runner;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;

import java.util.List;

/**
 * Whoever can actually execute a test case.
 * <p>
 * The core owns everything around a run - which cases are in it, what a stop
 * reaches, what each card paints - and nothing about starting one. Starting a
 * TestNG run means a {@code TestNGConfiguration} and a {@code PsiClass}, from
 * the TestNG and Java plugins, so the starting lives in the
 * {@code testin-testng} content module and arrives here (#144).
 * <p>
 * <b>One per framework.</b> A module running Robot Framework on PyCharm
 * contributes to this same point, and the core never learns there is a second
 * one.
 */
public interface TestRunner {

    /**
     * Contributed by a content module, which loads only where its framework's
     * plugin does. Empty everywhere else - an answer, not a missing one.
     */
    @NotNull ExtensionPointName<TestRunner> EP = ExtensionPointName.create("org.testin.testRunners");

    /**
     * UC-CODEGEN-008, Rule-CODEGEN-031.
     * <p>
     * Runs these cases as one run.
     */
    void run(final @NotNull Project p, final @NotNull List<TestCaseDto> cases);

    /**
     * Whoever can run tests here, and one that runs nothing when nobody can.
     * <p>
     * {@code OptionalPlugin.TESTNG} still decides whether Run is offered at all,
     * so the empty case is what happens if the guard is ever passed in an IDE
     * that cannot run - it says so in the log instead of throwing.
     */
    static @NotNull TestRunner available() {
        return EP.getExtensionList().stream()
                .findFirst()
                .orElseGet(() -> (p, cases) -> Logger.debug(
                        Bundle.message("runner.none", String.valueOf(cases.size()))));
    }
}
