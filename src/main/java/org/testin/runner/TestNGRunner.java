package org.testin.runner;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfigurationType;
import com.theoryinpractice.testng.model.TestType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Runs test cases through TestNG: any number of them, as one run.
 * <p>
 * One runner rather than one per shape. There were two - one setting
 * {@code TEST_OBJECT = METHOD} for a single case, one setting
 * {@code TEST_OBJECT = CLASS} for a whole test set - and they differed in little
 * else, while everything built on the first had to be built again on the second
 * or go without. The second went without: a test set run could not be stopped,
 * marked no card as running, and ran a different case when the one asked for had
 * no generated method (#36).
 * <p>
 * {@code TEST_OBJECT = PATTERN} takes a set of {@code Class,method} entries, so
 * one case and a hundred are the same call with a different number of entries.
 * What differs is the grouping, and that belongs to the caller: a card runs its
 * one case, so stopping it stops nothing else; a test set runs all of its cases
 * together, so it costs one process.
 * <p>
 * <b>The cases are carried through, not a class name.</b> That is what lets this
 * mark every card at the click, refuse a case whose method was never generated,
 * and hand the execution service the list it needs to put them back when the run
 * is stopped. The old class runner was handed a string, which is why it could do
 * none of those things.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class TestNGRunner {

    /**
     * Runs these cases as one run.
     * <p>
     * A case with no generated method is dropped and said out loud; the rest
     * still run. Nothing at all is started when none of them can be.
     */
    public void run(final @NotNull Project p, final @NotNull List<TestCaseDto> cases) {
        if (cases.isEmpty()) return;

        if (DumbService.isDumb(p)) {
            DumbService.getInstance(p).showDumbModeNotification(
                    "Cannot run tests while IntelliJ is indexing. Please wait a moment.");
            return;
        }

        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        // Marked here, where the tester's gesture is. Everything below hops to a
        // pooled thread and back, and a card that only turned Running when the
        // process finally existed would sit unchanged for a second after a click.
        cases.forEach(execution::starting);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ApplicationManager.getApplication().runReadAction(() -> prepare(p, cases));

            } catch (final IndexNotReadyException ex) {
                cases.forEach(execution::notStarting);

                ApplicationManager.getApplication().invokeLater(() -> DumbService.getInstance(p)
                        .showDumbModeNotification("Indexing interrupted the test run. Please try again."));
            }
        });
    }

    /**
     * Finds the generated method of each case under a read action, and hands the
     * launch back to the EDT.
     */
    private void prepare(final @NotNull Project p, final @NotNull List<TestCaseDto> cases) {
        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        final @NotNull List<TestCaseDto> found = new ArrayList<>();
        Optional<Module> module = Optional.empty();

        for (final TestCaseDto tc : cases) {
            final @NotNull Optional<PsiClass> owner = generatedClassOf(p, tc);

            if (owner.isEmpty()) {
                execution.noGeneratedCode(tc);
                continue;
            }

            found.add(tc);
            if (module.isEmpty()) {
                module = Optional.ofNullable(ModuleUtilCore.findModuleForPsiElement(owner.orElseThrow()));
            }
        }

        if (found.isEmpty()) return;

        final @NotNull Optional<Module> runModule = module;
        ApplicationManager.getApplication().invokeLater(() -> launch(p, found, runModule));
    }

    /**
     * Builds the configuration and starts it, for whichever of the cases the
     * tester still wants.
     */
    private void launch(final @NotNull Project p, final @NotNull List<TestCaseDto> found,
                        final @NotNull Optional<Module> module) {
        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        // Asked here rather than earlier: a case stopped while the run was being
        // prepared is left out of the pattern set instead of being started and
        // then killed a moment later.
        final @NotNull List<TestCaseDto> cases = execution.stillWanted(found);
        if (cases.isEmpty()) {
            Logger.info("Not starting: every case in the run was stopped before it began");
            return;
        }

        final @NotNull LinkedHashSet<String> patterns = new LinkedHashSet<>(cases.stream().map(TestNGRunner::patternFor).toList());
        final @NotNull String name = configNameFor(cases);

        final @NotNull RunManager runManager = RunManager.getInstance(p);
        final @NotNull TestNGConfigurationType configType = TestNGConfigurationType.getInstance();

        final @NotNull RunnerAndConfigurationSettings settings = Optional.ofNullable(runManager.findConfigurationByName(name))
                .orElseGet(() -> {
                    final @NotNull RunnerAndConfigurationSettings created =
                            runManager.createConfiguration(name, configType.getConfigurationFactories()[0]);
                    runManager.addConfiguration(created);
                    return created;
                });

        final @NotNull TestNGConfiguration configuration = (TestNGConfiguration) settings.getConfiguration();
        configuration.getPersistantData().TEST_OBJECT = TestType.PATTERN.getType();
        configuration.getPersistantData().setPatterns(patterns);
        configuration.setAllowRunningInParallel(true);

        module.ifPresent(configuration::setModule);

        runManager.setTemporaryConfiguration(settings);
        runManager.setSelectedConfiguration(settings);

        Logger.info("Running as '" + name + "': " + patterns);
        execution.launch(cases, settings);
    }

    /**
     * The generated class holding this case's method, and empty when either the
     * class or the method is not there.
     * <p>
     * The method is checked, not only the class: a configuration names a method,
     * and TestNG runs the whole class when the method it names is not in it - so
     * a case with no generated code used to run whatever else the class held and
     * report on that (#34).
     */
    private @NotNull Optional<PsiClass> generatedClassOf(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        final @NotNull List<String> fqcn = Fqcn.ofMethod(tc);
        if (fqcn.size() < 2) return Optional.empty();

        final @NotNull String classFqcn = String.join(".", fqcn.subList(0, fqcn.size() - 1));
        final @NotNull Optional<PsiClass> owner = Optional.ofNullable(
                JavaPsiFacade.getInstance(p).findClass(classFqcn, GlobalSearchScope.projectScope(p)));

        if (owner.isEmpty()) {
            Logger.warn("No generated class " + classFqcn + " for '" + tc.getDescription() + "'");
            return Optional.empty();
        }

        if (owner.orElseThrow().findMethodsByName(fqcn.getLast(), false).length == 0) {
            Logger.warn("No generated method " + fqcn.getLast() + " in " + classFqcn);
            return Optional.empty();
        }

        return owner;
    }

    /**
     * One entry of the pattern set: the class and the method, which is the form
     * the TestNG plugin splits on a comma.
     */
    private static @NotNull String patternFor(final @NotNull TestCaseDto tc) {
        final @NotNull List<String> fqcn = Fqcn.ofMethod(tc);

        return String.join(".", fqcn.subList(0, fqcn.size() - 1)) + "," + fqcn.getLast();
    }

    /**
     * What the run is called, which is also how a stop finds its process.
     * <p>
     * One case keeps the name a single run has always had. A whole test set takes
     * its class's name, because that is what the tester ran. A selection spanning
     * classes says how many, since no one name is true of it.
     */
    private static @NotNull String configNameFor(final @NotNull List<TestCaseDto> cases) {
        final @NotNull List<String> classes = cases.stream().map(TestNGRunner::simpleClassOf).distinct().toList();

        if (cases.size() == 1) return classes.getFirst() + "." + Fqcn.ofMethod(cases.getFirst()).getLast();
        if (classes.size() == 1) return classes.getFirst();

        return classes.getFirst() + " and " + (classes.size() - 1) + " more";
    }

    private static @NotNull String simpleClassOf(final @NotNull TestCaseDto tc) {
        final @NotNull List<String> fqcn = Fqcn.ofMethod(tc);

        return fqcn.get(fqcn.size() - 2);
    }
}
