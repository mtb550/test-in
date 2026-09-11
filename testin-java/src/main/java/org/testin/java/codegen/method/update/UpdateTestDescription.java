package org.testin.java.codegen.method.update;

import org.testin.java.codegen.JavaLiteral;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import org.testin.codegen.GenAction;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.NameSanitizer;

public class UpdateTestDescription extends UpdateTestBase implements GenAction {

    // UC-CODEGEN-010, Rule-CODEGEN-039, Rule-CODEGEN-040
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;
        applyOrCreate(p, tc, "Update Test Case Description", pm -> writeDescription(p, pm, tc));
    }

    /**
     * A selection of cases as one command and one undo entry - see
     * {@link UpdateTestBase#applyToEach} for what that is worth and what it
     * cost before (#66, finding 56).
     */
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        applyToEach(p, items, "Update Test Case Description", (pm, tc) -> writeDescription(p, pm, tc));
    }
}
