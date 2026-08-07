package org.testin.generateJavaCode.method;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.testin.generateJavaCode.GeneratorAction;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.Tools;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import java.util.List;

public class RenameTestMethod implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        final List<String> fqcn = Services.getInstance(p, Tools.class).buildFqcnMethod(tc);
        if (fqcn.size() < 2) return;
        final String path = String.join(".", fqcn.subList(0, fqcn.size() - 1));

        WriteCommandAction.runWriteCommandAction(p, "Rename Test Method", null, () -> {
            final PsiClass targetClass = JavaPsiFacade.getInstance(p).findClass(path, GlobalSearchScope.projectScope(p));
            if (targetClass == null) {
                Logger.warn("RenameTestMethod: class not found: " + path);
                return;
            }

            final String targetId = tc.getId().toString();
            for (PsiMethod m : targetClass.getMethods()) {
                final PsiAnnotation annotation = m.getModifierList().findAnnotation("org.testng.annotations.Test");
                if (annotation != null && annotation.getText().contains("testName") && annotation.getText().contains(targetId)) {
                    final String newName = Services.getInstance(p, Tools.class).sanitizeMethodName(tc.getDescription());
                    if (!m.getName().equals(newName)) {
                        m.setName(newName);
                    }
                    Logger.info("Renamed test method to: " + newName);
                    return;
                }
            }
            Logger.warn("RenameTestMethod: no method found with testName=" + tc.getId());
        });

    }


}
