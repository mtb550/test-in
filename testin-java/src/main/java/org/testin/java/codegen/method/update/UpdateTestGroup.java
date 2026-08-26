package org.testin.java.codegen.method.update;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenAction;
import org.testin.model.Group;
import org.testin.model.dto.TestCaseDto;

import java.util.List;

public class UpdateTestGroup extends UpdateTestBase implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        applyUpdate(p, tc, "Update Test Case Group", pm -> {
            final @NotNull List<String> activeGroups = tc.getGroup().stream()
                    .filter(g -> g != Group.UNASSIGNED)
                    .map(g -> "\"" + g.getName() + "\"")
                    .toList();

            final @NotNull String newValue = "{" + String.join(", ", activeGroups) + "}";
            updateTestAnnotationAttribute(p, pm, "groups", newValue);
        });
    }
}
