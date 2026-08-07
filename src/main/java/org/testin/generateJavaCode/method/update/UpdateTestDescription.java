package org.testin.generateJavaCode.method.update;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.testin.generateJavaCode.GeneratorAction;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Tools;

public class UpdateTestDescription extends UpdateTestBase implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;
        applyUpdate(p, tc, "Update Test Case Description", pm -> updateDescription(p, pm, tc));
    }

    private void updateDescription(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull TestCaseDto tc) {
        final PsiAnnotation testAnnotation = getTestAnnotation(pm);
        if (testAnnotation == null) return;

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(p);
        String newValue = "\"" + tc.getDescription().replace("\"", "\\\"") + "\"";
        updateAnnotationAttribute(factory, testAnnotation, "description", newValue);

        final String newMethodName = Services.getInstance(p, Tools.class).sanitizeMethodName(tc.getDescription());
        if (!pm.getName().equals(newMethodName)) {
            pm.setName(newMethodName);
        }
        CodeStyleManager.getInstance(p).reformat(pm);
    }
}
