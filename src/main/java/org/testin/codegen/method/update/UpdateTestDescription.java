package org.testin.codegen.method.update;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenAction;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Tools;

public class UpdateTestDescription extends UpdateTestBase implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;
        applyUpdate(p, tc, "Update Test Case Description", pm -> updateDescription(p, pm, tc));
    }

    private void updateDescription(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull TestCaseDto tc) {
        final String newValue = "\"" + tc.getDescription().replace("\"", "\\\"") + "\"";
        updateTestAnnotationAttribute(p, pm, "description", newValue);

        final String newMethodName = Services.getInstance(p, Tools.class).sanitizeMethodName(tc.getDescription());
        if (!pm.getName().equals(newMethodName)) {
            pm.setName(newMethodName);
        }
    }
}
