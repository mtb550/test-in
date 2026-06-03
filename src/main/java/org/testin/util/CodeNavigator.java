package org.testin.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.util.notifications.Notifier;

import java.util.List;

public class CodeNavigator {

    public void toCode(final @NotNull Project project, final @NotNull List<String> fqcn) {
        final String className = String.join(".", fqcn.subList(0, fqcn.size() - 1));
        final String methodName = fqcn.getLast();

        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {

                    final PsiClass targetClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.projectScope(project));

                    if (targetClass != null) {
                        Navigatable targetElement = targetClass;

                        final PsiMethod[] exactMethods = targetClass.findMethodsByName(methodName, false);

                        if (exactMethods.length > 0)
                            targetElement = exactMethods[0];

                        else
                            for (PsiMethod method : targetClass.getMethods()) {
                                if (method.getName().equalsIgnoreCase(methodName)) {
                                    targetElement = method;
                                    break;
                                }
                            }

                        final Navigatable finalTarget = targetElement;
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (finalTarget.canNavigate())
                                finalTarget.navigate(true);
                        });

                    } else
                        ApplicationManager.getApplication().invokeLater(() -> Notifier.getInstance().error(project, "Navigation Error: ", "Class Not Found: " + className));
                })
        );
    }
}