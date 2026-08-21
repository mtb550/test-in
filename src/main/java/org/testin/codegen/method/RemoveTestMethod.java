package org.testin.codegen.method;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenAction;
import org.testin.codegen.method.update.UpdateTestBase;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;

public class RemoveTestMethod extends UpdateTestBase implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        applyRemoval(p, tc, "Remove Test Method", pm -> {
            final @NotNull String name = pm.getName();
            pm.delete();
            Logger.info("Removed test method: " + name);
        });
    }
}
