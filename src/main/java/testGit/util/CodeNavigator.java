package testGit.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.pom.Navigatable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;

import java.util.List;

public class CodeNavigator {

    public static void toCode(final @NotNull List<String> fqcn, final @NotNull String testCaseName) {
        if (fqcn.isEmpty()) {
            Messages.showWarningDialog("This test case has no automation reference.", "Missing Reference");
            return;
        }

        final Project project = Config.getProject();
        final String methodName = Tools.toCamelCase(testCaseName);

        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {

                    String fqcnString = String.join(".", fqcn);
                    System.out.println("fqcn: " + fqcnString);

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
                                Messages.showErrorDialog("Could not find class in project:\n" + fqcn, "Class Not Found")
                        );
                    }
                })
        );
    }
}