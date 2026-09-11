package org.testin.java.codegen.method.update;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenAction;
import org.testin.model.dto.TestCaseDto;

import java.util.List;

public class UpdateTestGroup extends UpdateTestBase implements GenAction {

    // UC-CODEGEN-012, Rule-CODEGEN-045
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        applyUpdate(p, tc, "Update Test Case Group", pm -> writeGroups(p, pm, tc));
    }

    /**
     * A selection of cases as one command and one undo entry - see
     * {@link UpdateTestBase#applyToEach} for what that is worth and what it
     * cost before (#66, finding 56).
     */
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        applyToEach(p, items, "Update Test Case Group", (pm, tc) -> writeGroups(p, pm, tc));
    }
}
