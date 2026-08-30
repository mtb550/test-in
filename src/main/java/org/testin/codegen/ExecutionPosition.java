package org.testin.codegen;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.testcase.TestCaseOrder;

/**
 * Where a test case sits in its test set, as the number its generated method
 * carries so a run executes in the order the tester arranged.
 * <p>
 * One owner, because two generators need the same answer and a disagreement
 * between them is invisible: the one that writes a method and the one that
 * rewrites it when the set is reordered. If they differ, dragging a case fixes
 * the order and editing anything else quietly undoes it.
 * <p>
 * A position rather than the order itself. An order is a rank - a string with
 * room between any two of them, so a drag rewrites the one case that moved
 * instead of renumbering the set. A test framework wants a number, and the only
 * number that means the same thing is the place that rank sorts into.
 * <p>
 * In the core rather than beside the Java generator: a second language module
 * ordering its own tests needs this same answer, and the rule about what order
 * a run executes in is not Java's to decide.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecutionPosition {

    /**
     * This case's place among the cases of its test set, counting from one.
     * <p>
     * Asked of the set rather than of whatever list a caller happens to hold: a
     * generator can be given a subset - three cases of fifty, an import of one
     * sheet - and an index into that would be a different number for the same
     * case depending on how it was generated.
     * <p>
     * A case the index has not seen yet runs last. It is being created, and the
     * end is where a tester looks for something that has just arrived.
     */
    public static int of(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        return TestCaseOrder.positionOf(TestCaseOrder.ordered(
                Services.getInstance(p, ProjectIndexer.class).getTestCasesForTestSet(tc.getParent().getPath())), tc);
    }
}
