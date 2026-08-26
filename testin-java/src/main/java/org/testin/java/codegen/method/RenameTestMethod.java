package org.testin.java.codegen.method;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenAction;
import org.testin.java.codegen.method.update.UpdateTestBase;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.NameSanitizer;

public class RenameTestMethod extends UpdateTestBase implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        applyUpdate(p, tc, "Rename Test Method", pm -> {
            final @NotNull String newName = NameSanitizer.methodName(tc.getDescription());
            if (!pm.getName().equals(newName)) {
                pm.setName(newName);
            }
            Logger.info("Renamed test method to: " + newName);
        });
    }
}
