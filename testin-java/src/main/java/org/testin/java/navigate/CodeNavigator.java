package org.testin.java.navigate;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.navigate.CodeNavigation;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Finds the generated method and opens it, through the Java plugin's PSI.
 * <p>
 * Lives in the content module rather than the core jar: JavaPsiFacade, PsiClass
 * and PsiMethod exist only where the Java plugin does, and in the core jar the
 * verifier reported every reference to them against PyCharm, GoLand and
 * WebStorm (#144). The core asks {@link CodeNavigation#available()} instead,
 * which answers with this where the module loaded and with a no-op where it did
 * not.
 */
public final class CodeNavigator implements CodeNavigation {

    /**
     * UC-CODEGEN-006, Rule-CODEGEN-026.
     * <p>
     * The generated method that runs this case, and empty when there is none.
     * <p>
     * The class comes from the tree path, which is what names it; the method
     * comes from the id in its {@code @Test} annotation, which is what
     * identifies it. Both halves of this class ask this, so Run and Go-to-code
     * cannot disagree about which method a case owns.
     */
    private @NotNull Optional<PsiMethod> resolve(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        final @NotNull List<String> fqcn = Fqcn.ofMethod(tc);
        if (fqcn.size() < 2) return Optional.empty();

        final @NotNull String classFqcn = String.join(".", fqcn.subList(0, fqcn.size() - 1));
        final @NotNull Optional<PsiClass> owner = Optional.ofNullable(
                JavaPsiFacade.getInstance(p).findClass(classFqcn, GlobalSearchScope.projectScope(p)));

        if (owner.isEmpty()) {
            Logger.warn("No generated class " + classFqcn + " for '" + tc.getDescription() + "'");
            return Optional.empty();
        }

        final @NotNull Optional<PsiMethod> method = GeneratedMethod.forCase(owner.orElseThrow(), tc);
        if (method.isEmpty()) Logger.warn("No generated method for '" + tc.getDescription() + "' in " + classFqcn);

        return method;
    }

    /**
     * UC-CODEGEN-006, Rule-CODEGEN-026.
     * <p>
     * The cases a generated method carries, and whether that method does
     * anything, one pass per class.
     * <p>
     * Grouped by the class each case generates into, so a page of one test set
     * resolves one class and walks its methods once. {@code byCaseId} already
     * returns every generated method of a class keyed by the id it carries, so
     * the per-case half of this is a map lookup. Asking {@code methodOf} per
     * card would resolve the same class once per card instead.
     * <p>
     * A class that cannot be resolved contributes nothing rather than failing:
     * its cases have no method, which is what the caller is asking. The caller
     * holds the read action.
     * <p>
     * A body with no statements is the stub {@code CreateTestMethod} writes - an
     * annotation, a name and a TODO comment. Reporting that as automated made
     * every test case with a description look automated, which is every test
     * case anyone had finished writing.
     */
    @Override
    public @NotNull Map<UUID, Boolean> methodsFor(final @NotNull Project p, final @NotNull List<TestCaseDto> cases) {
        final @NotNull Map<String, List<TestCaseDto>> byClass = new LinkedHashMap<>();

        for (final TestCaseDto tc : cases) {
            final @NotNull List<String> fqcn = Fqcn.ofMethod(tc);
            if (fqcn.size() < 2) continue;

            byClass.computeIfAbsent(String.join(".", fqcn.subList(0, fqcn.size() - 1)),
                    ignored -> new ArrayList<>()).add(tc);
        }

        final @NotNull Map<UUID, Boolean> found = new LinkedHashMap<>();

        for (final Map.Entry<String, List<TestCaseDto>> group : byClass.entrySet()) {
            final @NotNull Optional<PsiClass> owner = Optional.ofNullable(
                    JavaPsiFacade.getInstance(p).findClass(group.getKey(), GlobalSearchScope.projectScope(p)));

            if (owner.isEmpty()) continue;

            final @NotNull Map<String, PsiMethod> methods = GeneratedMethod.byCaseId(owner.orElseThrow());

            for (final TestCaseDto tc : group.getValue()) {
                Optional.ofNullable(methods.get(tc.getId().toString()))
                        .ifPresent(pm -> found.put(tc.getId(), doesSomething(pm)));
            }
        }

        return found;
    }

    /**
     * Whether a generated method has anything in it.
     * <p>
     * Statements, not text: the stub Testin writes carries a TODO comment, and a
     * comment is not a statement - so an untouched stub answers false and a
     * method a tester has written one line into answers true. A method with no
     * body at all is abstract or from a class file, and has nothing either.
     */
    private static boolean doesSomething(final @NotNull PsiMethod pm) {
        return Optional.ofNullable(pm.getBody())
                .filter(body -> body.getStatements().length > 0)
                .isPresent();
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
                            // The same sentence Run gives, from the same owner.
                            // This said "Nothing to open" where Run said "has no
                            // generated code yet" - one state described two ways,
                            // one keystroke apart (#246).
                            ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(p, Notifier.class)
                                    .softShowNoGeneratedCode(p, tc.getDescription()));
                            return;
                        }

                        final @NotNull Navigatable target = found.orElseThrow();
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (target.canNavigate()) target.navigate(true);
                        });

                    } catch (final IndexNotReadyException ex) {
                        Logger.trace("index not ready, deferring navigation");
                        // Notifications must not be raised from inside a read action on a pooled thread.
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).softShow(p, "Waiting for indexing"));
                        DumbService.getInstance(p).runWhenSmart(() -> toCode(p, tc));
                    }
                })
        );
    }
}
