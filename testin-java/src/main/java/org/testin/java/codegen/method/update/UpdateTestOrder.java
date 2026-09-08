package org.testin.java.codegen.method.update;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.codegen.ExecutionPosition;
import org.testin.codegen.GenAction;
import org.testin.codegen.Fqcn;
import org.testin.java.codegen.GeneratedMethod;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Puts the generated methods in the order the tester arranged: the position into
 * each method's {@code priority}, and each method into its place in the file.
 * <p>
 * The attribute is called priority because that is TestNG's name for what
 * decides execution order. What it carries is the case's position, which is why
 * this fires on a reorder and not on a change to the case's own High/Medium/Low
 * - that decides nothing about running and writes nothing into the code
 * (Rule-CODEGEN-014).
 * <p>
 * The attribute is what a run obeys. Moving the method is for the person
 * reading the file, so that a class read top to bottom is the test set read top
 * to bottom (#242 follow-up).
 */
public class UpdateTestOrder extends UpdateTestBase implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (obj instanceof TestCaseDto tc) executeAll(p, List.of(tc));
    }

    /**
     * UC-CODEGEN-011, Rule-CODEGEN-042, Rule-CODEGEN-067.
     * <p>
     * Every case in the sets these belong to, not only the ones handed in.
     * <p>
     * A position is a number with no room between two of them, so moving one
     * case past three others changes where all four sit. A caller that knows it
     * rearranged a whole set can hand the set over and this costs nothing; one
     * that moved a single case - the update menu's Order field - would otherwise
     * write that case's new number and leave every case it jumped carrying the
     * number it had before.
     * <p>
     * <b>One command for the whole set.</b> Each case used to be its own
     * {@code invokeLater} and its own write command, so dragging one card in a
     * set of 120 meant 120 events, 120 class lookups and 120 entries in the
     * IDE's undo - one CTRL+Z per case to take back one drag. That is the
     * mistake {@code CreateTestMethod} records under #51, and this is the same
     * fix: resolve the class once and do the set inside one command.
     */
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        final @NotNull Map<Path, List<TestCaseDto>> sets = new LinkedHashMap<>();

        for (final Object item : items) {
            if (item instanceof TestCaseDto tc) sets.computeIfAbsent(tc.getParent().getPath(), path -> ExecutionPosition.setOf(p, tc));
        }
        if (sets.isEmpty()) return;

        final @NotNull List<List<TestCaseDto>> ordered = new ArrayList<>(sets.values());

        // Handed to a later event and not waited for: a drag has redrawn the
        // tree and the editor by the time this runs, and the tester is already
        // doing the next thing. Nothing downstream reads the code back.
        ApplicationManager.getApplication().invokeLater(() ->
                WriteCommandAction.runWriteCommandAction(p, "Update Test Case Order", null,
                        () -> ordered.forEach(inSet -> arrange(p, inSet))));
    }

    /**
     * UC-CODEGEN-011, Rule-CODEGEN-043, Rule-CODEGEN-067.
     * <p>
     * One set: every case's position written into its method, and every method
     * put after the one before it.
     * <p>
     * A case with no method is passed over rather than reported, because the
     * sweep touches every case in the set and a set half written would say so
     * once per case (Rule-CODEGEN-044).
     */
    private void arrange(final @NotNull Project p, final @NotNull List<TestCaseDto> inSet) {
        if (inSet.isEmpty()) return;

        final @NotNull Optional<PsiClass> target = classOf(p, inSet.getFirst());
        if (target.isEmpty()) return;

        final @NotNull PsiClass pc = target.get();

        // Read once for the set, not once per case. Asking the class per case
        // walked every method in it for every case, which is the whole class
        // squared for one drag.
        final @NotNull Map<String, PsiMethod> methods = GeneratedMethod.byCaseId(pc);

        @Nullable PsiElement after = null;
        int position = 0;

        for (final TestCaseDto tc : inSet) {
            position++;

            final @Nullable PsiMethod pm = methods.get(tc.getId().toString());
            if (pm == null) continue;

            updateTestAnnotationAttribute(p, pm, "priority", String.valueOf(position));
            after = place(pc, pm, after);
        }
    }

    /**
     * UC-CODEGEN-011, Rule-CODEGEN-067.
     * <p>
     * Puts one method where it belongs and answers with the method now in the
     * file, which is what the next one goes after.
     * <p>
     * <b>Among the generated methods, not among all of them.</b> The first case
     * goes before whichever generated method currently sits highest, so the
     * tester's own members - a field, a {@code @BeforeMethod}, a helper written
     * above the generated block - are not stepped over or shuffled. Testin
     * arranges what Testin wrote.
     * <p>
     * <b>A method already in place is left alone,</b> which is what keeps this
     * off the diff: a reorder that moved one card rewrites one method rather
     * than the file. Moving is add-then-delete because PSI has no move; the
     * added copy is the element that survives, so it is what comes back.
     */
    private static @Nullable PsiElement place(final @NotNull PsiClass pc, final @NotNull PsiMethod pm, final @Nullable PsiElement after) {
        if (after == null) {
            final @Nullable PsiMethod first = firstGenerated(pc);

            if (first == null || first == pm) return pm;

            final @NotNull PsiElement moved = pc.addBefore(pm, first);
            pm.delete();
            return moved;
        }

        if (nextAfter(after) == pm) return pm;

        final @NotNull PsiElement moved = pc.addAfter(pm, after);
        pm.delete();
        return moved;
    }

    /**
     * The generated method that sits highest in the class, and null in a class
     * that holds none.
     */
    private static @Nullable PsiMethod firstGenerated(final @NotNull PsiClass pc) {
        for (final PsiMethod pm : pc.getMethods()) {
            if (GeneratedMethod.testAnnotationOf(pm).isPresent()) return pm;
        }

        return null;
    }

    /**
     * What follows an element once the blank lines between them are stepped
     * over. Whitespace only: a comment the tester wrote is something, and a
     * method sitting after one is not where this would have put it.
     */
    private static @Nullable PsiElement nextAfter(final @NotNull PsiElement element) {
        PsiElement next = element.getNextSibling();
        while (next instanceof PsiWhiteSpace) next = next.getNextSibling();

        return next;
    }

}
