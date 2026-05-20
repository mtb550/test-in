package org.testin.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.pom.Navigatable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;

import java.util.List;
import java.util.stream.Collectors;

public class CodeNavigator {

    public void toCode(final @NotNull List<String> rawFqcn, final @NotNull String testCaseName) {
        if (rawFqcn.isEmpty()) {
            Messages.showWarningDialog("This test case has no automation reference.", "Missing Reference");
            return;
        }

        final Project project = Config.getProject();

        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {

                    List<String> cleanedFqcn = Tools.getInstance().sanitizeFqcn(rawFqcn);

                    if (cleanedFqcn.isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showErrorDialog("Invalid FQCN path format.", "Navigation Error")
                        );
                        return;
                    }

                    String methodName;
                    String baseClassName;
                    String targetMethodName = Tools.getInstance().formatMethodName(testCaseName);

                    if (cleanedFqcn.size() >= 2 && (cleanedFqcn.get(cleanedFqcn.size() - 2).toLowerCase().endsWith("test") || cleanedFqcn.getLast().equalsIgnoreCase(targetMethodName))) {
                        methodName = cleanedFqcn.removeLast();
                        baseClassName = cleanedFqcn.removeLast();
                    } else {
                        methodName = targetMethodName;
                        baseClassName = cleanedFqcn.removeLast();
                    }

                    String expectedClassName = Tools.getInstance().toPascalCase(baseClassName);
                    if (expectedClassName.toLowerCase().endsWith("test")) {
                        if (!expectedClassName.endsWith("Test")) {
                            expectedClassName = expectedClassName.substring(0, expectedClassName.length() - 4) + "Test";
                        }
                    } else {
                        expectedClassName += "Test";
                    }

                    String packageName = cleanedFqcn.stream()
                            .map(String::toLowerCase)
                            .collect(Collectors.joining("."));

                    String fqcnString = packageName.isEmpty() ? expectedClassName : packageName + "." + expectedClassName;

                    System.out.println("[NAVIGATOR] Searching for cleaned FQCN: " + fqcnString);
                    System.out.println("[NAVIGATOR] Looking for method: " + methodName);

                    PsiClass targetClass = JavaPsiFacade.getInstance(project)
                            .findClass(fqcnString, GlobalSearchScope.projectScope(project));

                    if (targetClass != null) {
                        Navigatable targetElement = targetClass;
                        PsiMethod[] exactMethods = targetClass.findMethodsByName(methodName, false);

                        if (exactMethods.length > 0) {
                            targetElement = exactMethods[0];
                        } else {
                            for (PsiMethod method : targetClass.getMethods()) {
                                if (method.getName().equalsIgnoreCase(methodName)) {
                                    targetElement = method;
                                    break;
                                }
                            }
                        }

                        final Navigatable finalTarget = targetElement;
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (finalTarget.canNavigate()) {
                                finalTarget.navigate(true);
                            }
                        });

                    } else {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showErrorDialog("Could not find class: " + fqcnString, "Class Not Found")
                        );
                    }
                })
        );
    }
}