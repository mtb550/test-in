package org.testin.codegen.method.update;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenAction;
import org.testin.mappers.dto.TestCaseDto;

public class UpdateTestPriority extends UpdateTestBase implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        applyUpdate(p, tc, "Update Test Case Priority", pm ->
                updateTestAnnotationAttribute(p, pm, "priority", String.valueOf(tc.getPriority().getValue())));
    }
}
