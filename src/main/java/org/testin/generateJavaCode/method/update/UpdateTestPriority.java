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

public class UpdateTestPriority extends UpdateTestBase implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;
        applyUpdate(p, tc, "Update Test Case Priority", pm -> updatePriority(p, pm, tc));
    }

    private void updatePriority(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull TestCaseDto tc) {
        final PsiAnnotation testAnnotation = getTestAnnotation(pm);
        if (testAnnotation == null) return;

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(p);
        String newValue = String.valueOf(tc.getPriority().getValue());
        updateAnnotationAttribute(factory, testAnnotation, "priority", newValue);
        CodeStyleManager.getInstance(p).reformat(pm);
    }
}
