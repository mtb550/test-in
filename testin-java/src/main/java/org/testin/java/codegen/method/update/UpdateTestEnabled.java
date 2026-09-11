package org.testin.java.codegen.method.update;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import java.util.List;
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

    // UC-CODEGEN-013, Rule-CODEGEN-047, Rule-CODEGEN-048
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        applyUpdate(p, tc, "Update Test Case Enabled", pm -> writeEnabled(p, pm, tc));
    }

    /**
     * A selection of cases as one command and one undo entry - see
     * {@link UpdateTestBase#applyToEach} for what that is worth and what it
     * cost before (#66, finding 56).
     */
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        applyToEach(p, items, "Update Test Case Enabled", (pm, tc) -> writeEnabled(p, pm, tc));
    }
}
