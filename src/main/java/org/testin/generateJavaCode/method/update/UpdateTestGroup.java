package org.testin.generateJavaCode.method.update;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.Group;
import org.testin.generateJavaCode.GeneratorAction;
import org.testin.mappers.dto.TestCaseDto;

import java.util.List;

public class UpdateTestGroup extends UpdateTestBase implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;
        applyUpdate(p, tc, "Update Test Case Group", pm -> updateGroup(p, pm, tc));
    }

    private void updateGroup(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull TestCaseDto tc) {
        final PsiAnnotation testAnnotation = getTestAnnotation(pm);
        if (testAnnotation == null) return;

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(p);
        List<String> activeGroups = tc.getGroup().stream()
                .filter(g -> g != Group.UNASSIGNED)
                .map(g -> "\"" + g.getName() + "\"")
                .toList();

        String newValue;
        if (activeGroups.isEmpty()) {
            newValue = "{}";
        } else {
            newValue = "{" + String.join(", ", activeGroups) + "}";
        }
        updateAnnotationAttribute(factory, testAnnotation, "groups", newValue);
        CodeStyleManager.getInstance(p).reformat(pm);
    }
}
