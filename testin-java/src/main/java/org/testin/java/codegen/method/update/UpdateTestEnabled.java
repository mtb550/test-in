package org.testin.java.codegen.method.update;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenAction;
import org.testin.model.TestCaseStatus;
import org.testin.model.dto.TestCaseDto;

/**
 * A disabled test case stops running.
 * <p>
 * {@code DISABLED} is the one status that says something about whether a case
 * should run, and it used to say it only to Testin: the card showed it, the JSON
 * stored it, and the generated method ran exactly as before. The suite and the
 * status disagreed, and only the suite was believed (#166).
 * <p>
 * Leaving {@code DISABLED} takes the attribute off rather than writing
 * {@code enabled = true}. That is what {@code @Test} already means, and writing
 * it would leave every case that was ever disabled carrying a word that says
 * nothing - one more thing on the annotation for a reader to wonder about.
 */
public class UpdateTestEnabled extends UpdateTestBase implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        applyUpdate(p, tc, "Update Test Case Enabled", pm -> updateEnabled(p, pm, tc));
    }

    private void updateEnabled(final @NotNull Project p, final @NotNull PsiMethod pm, final @NotNull TestCaseDto tc) {
        if (tc.getStatus() == TestCaseStatus.DISABLED) updateTestAnnotationAttribute(p, pm, "enabled", "false");
        else removeTestAnnotationAttribute(p, pm, "enabled");
    }
}
