package org.testin.java.codegen.method.update;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.ExecutionPosition;
import org.testin.codegen.GenAction;
import org.testin.model.dto.TestCaseDto;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Every case in the sets these belong to, not only the ones handed in.
     * <p>
     * A position is a number with no room between two of them, so moving one
     * case past three others changes where all four sit. A caller that knows it
     * rearranged a whole set can hand the set over and this costs nothing; one
     * that moved a single case - the update menu's Order field - would otherwise
     * write that case's new number and leave every case it jumped carrying the
     * number it had before.
     * <p>
     * Asked per set rather than per case so a set handed over whole is looked up
     * once, and generated once, however many of its cases the caller passed.
     */
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        final @NotNull Map<Path, List<TestCaseDto>> sets = new LinkedHashMap<>();

        for (final Object item : items) {
            if (item instanceof TestCaseDto tc) sets.computeIfAbsent(tc.getParent().getPath(), path -> ExecutionPosition.setOf(p, tc));
        }

        for (final List<TestCaseDto> inSet : sets.values()) {
            for (final TestCaseDto tc : inSet) execute(p, tc);
        }
    }
}
