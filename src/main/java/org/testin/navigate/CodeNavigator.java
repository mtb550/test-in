package org.testin.navigate;

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
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.util.List;
import java.util.Optional;

public class CodeNavigator {

    public void toCode(final @NotNull Project p, final @NotNull List<String> fqcn) {
        final @NotNull String className = String.join(".", fqcn.subList(0, fqcn.size() - 1));
        final @NotNull String methodName = fqcn.getLast();

        Logger.trace("org.testin.navigate to method, className: " + className + ", methodName: " + methodName);

        if (DumbService.isDumb(p)) {
            Logger.trace("dumb mode detected, deferring navigation");
            DumbService.getInstance(p).runWhenSmart(() -> toCode(p, fqcn));
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {
                    try {
                        final @NotNull Optional<PsiClass> found = Optional.ofNullable(
                                JavaPsiFacade.getInstance(p).findClass(className, GlobalSearchScope.projectScope(p)));

                        if (found.isPresent()) {
                            final @NotNull PsiClass targetClass = found.orElseThrow();
                            Navigatable targetElement = targetClass;

                            final PsiMethod @NotNull[] exactMethods = targetClass.findMethodsByName(methodName, false);

                            if (exactMethods.length > 0)
                                targetElement = exactMethods[0];

                            else
                                for (PsiMethod method : targetClass.getMethods()) {
                                    if (method.getName().equalsIgnoreCase(methodName)) {
                                        targetElement = method;
                                        break;
                                    }
                                }

                            final @NotNull Navigatable finalTarget = targetElement;
                            ApplicationManager.getApplication().invokeLater(() -> {
                                if (finalTarget.canNavigate())
                                    finalTarget.navigate(true);
                            });

                        } else
                            ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(p, Notifier.class).softShow(p, "Navigation Failed", "Class not found: " + className));

                    } catch (final IndexNotReadyException ex) {
                        Logger.trace("index not ready, deferring navigation");
                        // Notifications must not be raised from inside a read action on a pooled thread.
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).softShow(p, "Waiting for indexing"));
                        DumbService.getInstance(p).runWhenSmart(() -> toCode(p, fqcn));
                    }
                })
        );
    }
}
