package org.testin.generateJavaCode.method.update;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.Group;
import org.testin.generateJavaCode.GeneratorAction;
import org.testin.mappers.dto.TestCaseDto;

import java.util.List;

public class UpdateTestGroup extends UpdateTestBase implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        applyUpdate(p, tc, "Update Test Case Group", pm -> {
            final List<String> activeGroups = tc.getGroup().stream()
                    .filter(g -> g != Group.UNASSIGNED)
                    .map(g -> "\"" + g.getName() + "\"")
                    .toList();

            final String newValue = "{" + String.join(", ", activeGroups) + "}";
            updateTestAnnotationAttribute(p, pm, "groups", newValue);
        });
    }
}
