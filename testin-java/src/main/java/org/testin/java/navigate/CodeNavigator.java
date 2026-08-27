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
import java.util.List;
import java.util.Optional;

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

    @Override
    public @NotNull Optional<List<String>> methodOf(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        return resolve(p, tc).map(method -> {
            final @NotNull List<String> named = new ArrayList<>(Fqcn.ofMethod(tc));
            named.set(named.size() - 1, method.getName());

            return named;
        });
    }

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
                                    .softRefuse(p, "Nothing to open", "No automation has been generated for '" + tc.getDescription() + "' yet"));
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
