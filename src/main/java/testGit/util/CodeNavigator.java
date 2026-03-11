package testGit.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.pom.Navigatable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import testGit.pojo.Config;

public class CodeNavigator {

    public static void toCode(String fqcn, String testCaseName) {
        if (fqcn == null || fqcn.trim().isEmpty()) {
            Messages.showWarningDialog("This test case has no automation reference.", "Missing Reference");
            return;
        }

        // 1. Cache the project reference locally so we aren't repeatedly calling Config
        final Project project = Config.getProject();
        final String methodName = Tools.toCamelCase(testCaseName);

        // 2. ENHANCEMENT: Move all heavy PSI searching OFF the main UI thread!
        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            // 3. Request read access while on the background thread
            ApplicationManager.getApplication().runReadAction(() -> {

                // Fast cache lookup
                PsiClass targetClass = JavaPsiFacade.getInstance(project)
                        .findClass(fqcn, GlobalSearchScope.projectScope(project));

                if (targetClass != null) {
                    Navigatable targetElement = targetClass;

                    // Fast exact match lookup
                    PsiMethod[] exactMethods = targetClass.findMethodsByName(methodName, false);

                    if (exactMethods.length > 0) {
                        targetElement = exactMethods[0];
                    } else {
                        // Fallback: Case-insensitive search
                        for (PsiMethod method : targetClass.getMethods()) {
                            if (method.getName().equalsIgnoreCase(methodName)) {
                                targetElement = method;
                                break;
                            }
                        }
                    }

                    // 4. Jump back to the UI thread ONLY to open the editor window
                    final Navigatable finalTarget = targetElement;
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (finalTarget.canNavigate()) {
                            finalTarget.navigate(true);
                        }
                    });

                } else {
                    // Show error on UI thread if not found
                    ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showErrorDialog("Could not find class in project:\n" + fqcn, "Class Not Found")
                    );
                }
            });
        });
    }
}