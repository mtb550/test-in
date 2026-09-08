package org.testin.java.codegen.method.update;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenAction;
import org.testin.model.Group;
import org.testin.model.dto.TestCaseDto;

import java.util.List;

public class UpdateTestGroup extends UpdateTestBase implements GenAction {

    // UC-CODEGEN-012, Rule-CODEGEN-045
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        applyUpdate(p, tc, "Update Test Case Group", pm -> writeGroups(p, pm, tc));
    }
}
