package org.testin.java.codegen;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.PsiMethod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.util.LinkedHashMap;
import java.util.Map;
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
     * The attribute carrying the case's id, which is the only part of a
     * generated method that must never be edited.
     */
    private static final @NotNull String TEST_NAME = "testName";

    /**
     * Rule-CODEGEN-001.
     * <p>
     * The method in this class carrying this case's id, and empty when none
     * does.
     */
    public static @NotNull Optional<PsiMethod> forCase(final @NotNull PsiClass pc, final @NotNull TestCaseDto tc) {
        return Optional.ofNullable(byCaseId(pc).get(tc.getId().toString()));
    }

    /**
     * Rule-CODEGEN-001.
     * <p>
     * Every generated method in the class, by the case id it carries.
     * <p>
     * One pass for a caller with a set to place rather than one scan per case.
     * The order sweep asks for all of them - the position of every case in the
     * set, and where each method goes - so asking per case walked every method
     * in the class for every case in the set: 120 cases in one class is 14,400
     * scans of the same list for one drag.
     */
    public static @NotNull Map<String, PsiMethod> byCaseId(final @NotNull PsiClass pc) {
        final @NotNull Map<String, PsiMethod> byId = new LinkedHashMap<>();

        for (final PsiMethod pm : pc.getMethods()) {
            caseIdOf(pm).ifPresent(id -> byId.putIfAbsent(id, pm));
        }

        return byId;
    }

    /**
     * UC-CODEGEN-002, Rule-CODEGEN-013.
     * <p>
     * The case id in a method's {@code @Test}, and empty on a method that
     * carries none.
     * <p>
     * Read as the attribute rather than found in the annotation's text. It used
     * to ask whether the rendered annotation contained the word {@code testName}
     * and contained the id, which is true of an annotation that merely mentions
     * either - a description holding an id pasted into it answered for a case
     * that is not this one. It also rendered the whole annotation to a string
     * for every method of the class, for every case being asked about.
     */
    public static @NotNull Optional<String> caseIdOf(final @NotNull PsiMethod pm) {
        return testAnnotationOf(pm)
                .map(annotation -> annotation.findDeclaredAttributeValue(TEST_NAME))
                .filter(PsiLiteralValue.class::isInstance)
                .map(value -> ((PsiLiteralValue) value).getValue())
                .filter(String.class::isInstance)
                .map(String.class::cast);
    }

    /**
     * The method's {@code @Test} annotation, empty on a method that has none.
     */
    public static @NotNull Optional<PsiAnnotation> testAnnotationOf(final @NotNull PsiMethod pm) {
        return Optional.ofNullable(pm.getModifierList().findAnnotation("org.testng.annotations.Test"));
    }
}
