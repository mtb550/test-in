package org.testin.java.codegen;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.util.Arrays;
import java.util.Optional;

/**
 * Which generated method belongs to a test case.
 * <p>
 * By the id the generator wrote into the method's {@code @Test} annotation, not
 * by the method's name. The name is the case's description with the punctuation
 * taken out, so two cases whose descriptions differ only in punctuation or in
 * capitals - "Login" and "Log-in", "Add user" and "add user" - sanitize to the
 * same name and only one method is ever written. Asked by name, both cases
 * answer with that one method; asked by id, the second answers honestly that it
 * has none.
 * <p>
 * One owner because there were two answers to this question in the plugin and
 * they disagreed. The updaters asked by id and the runner asked by name, so the
 * runner collapsed the two cases into one pattern entry, TestNG ran the method
 * once, and the report carried the first case's id - the second case ran,
 * reported nothing and ended with no verdict at all. The same gap made a method
 * a tester renamed by hand invisible to Run while the updaters kept editing it.
 * <p>
 * Reads PSI, so a caller holds the read action.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GeneratedMethod {

    /**
     * The method in this class carrying this case's id, and empty when none
     * does.
     */
    public static @NotNull Optional<PsiMethod> forCase(final @NotNull PsiClass pc, final @NotNull TestCaseDto tc) {
        final @NotNull String targetId = tc.getId().toString();

        return Arrays.stream(pc.getMethods())
                .filter(method -> testAnnotationOf(method)
                        .map(PsiAnnotation::getText)
                        .filter(text -> text.contains("testName") && text.contains(targetId))
                        .isPresent())
                .findFirst();
    }

    /**
     * The method's {@code @Test} annotation, empty on a method that has none.
     */
    public static @NotNull Optional<PsiAnnotation> testAnnotationOf(final @NotNull PsiMethod pm) {
        return Optional.ofNullable(pm.getModifierList().findAnnotation("org.testng.annotations.Test"));
    }
}
