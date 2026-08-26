package org.testin.run;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;

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
                        "No test runner in this IDE; " + cases.size() + " case(s) not started"));
    }
}
