package org.testin.java.codegen.method.update;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.ExecutionPosition;
import org.testin.codegen.GenAction;
import org.testin.model.dto.TestCaseDto;

/**
 * Rewrites the priority attribute of a generated method when the case moves in
 * its set.
 * <p>
 * The attribute is called priority because that is TestNG's name for what
 * decides execution order. What it carries is the case's position, which is why
 * this fires on a reorder and no longer on a change to the case's own
 * High/Medium/Low - that decides nothing about running and writes nothing into
 * the code.
 */
public class UpdateTestOrder extends UpdateTestBase implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        applyIfGenerated(p, tc, "Update Test Case Order", pm ->
                updateTestAnnotationAttribute(p, pm, "priority", String.valueOf(ExecutionPosition.of(p, tc))));
    }
}
