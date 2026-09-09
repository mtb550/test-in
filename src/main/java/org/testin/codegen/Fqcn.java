package org.testin.codegen;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.util.NameSanitizer;

import java.util.ArrayList;
import java.util.List;

/**
 * The Java name generated from a node's place in the tree.
 * <p>
 * Every generated package, class and method name is one walk: the node's
 * {@code path2} with the test cases directory dropped and each remaining name
 * sanitized. Here rather than at the eight call sites that need it, so a package
 * cannot be derived one way by the creator and another by the renamer.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Fqcn {

    /**
     * UC-CODEGEN-002, Rule-CODEGEN-012.
     * <p>
     * The method name alone, for the callers that want only the tail of
     * {@link #ofMethod}. Four places derived it the same way, and a name says
     * what the last element of that list is.
     * <p>
     * Empty for a case that names no method, which is the same answer
     * {@link #ofMethod} gives as an empty list.
     */
    public static @NotNull String methodNameOf(final @NotNull TestCaseDto tc) {
        return NameSanitizer.methodName(tc.getDescription());
    }

    /**
     * UC-CODEGEN-002, Rule-CODEGEN-002.
     * <p>
     * Packages, class and method for the automation code of one test case - and
     * empty when there is no method to name.
     * <p>
     * A method is named by the description, so a case saved without one names
     * nothing. Returned empty rather than completed with a placeholder: every
     * caller here already skips on a list too short to split, which is the same
     * contract {@link #ofClass} answers under and the same one they were already
     * written for. A name invented here would be written into the tester's
     * source file (#155).
     */
    public static @NotNull ArrayList<String> ofMethod(final @NotNull TestCaseDto tc) {
        final @NotNull String methodName = NameSanitizer.methodName(tc.getDescription());
        if (methodName.isEmpty()) return new ArrayList<>();

        final @NotNull ArrayList<String> generatedFqcn = withoutTestCasesDir(tc.getParent().getPath2());

        if (generatedFqcn.isEmpty()) {
            generatedFqcn.add("DefaultTest");
        }

        sanitizeTail(generatedFqcn);
        generatedFqcn.add(methodName);

        return generatedFqcn;
    }

    /**
     * UC-CODEGEN-001, Rule-CODEGEN-007.
     * <p>
     * Packages and class for a directory, or empty when there is no class to
     * name.
     * <p>
     * Only the test cases directory itself names no class, and the four callers
     * all skip on empty (#66, F1). It said so in a message titled "Class Name
     * Unknown", which a tester removing or moving a test set read as a failure
     * to build something they had not asked for (#249).
     */
    public static @NotNull List<String> ofClass(final @NotNull DirectoryDto dir) {
        final @NotNull ArrayList<String> generatedFqcn = withoutTestCasesDir(dir.getPath2());

        if (generatedFqcn.isEmpty()) {
            Logger.info("No class name for '" + dir.getName() + "': it is the test cases directory itself");
            return List.of();
        }

        sanitizeTail(generatedFqcn);
        return generatedFqcn;
    }

    /**
     * UC-CODEGEN-001, Rule-CODEGEN-008.
     * <p>
     * Packages alone, for a directory that becomes one.
     */
    public static @NotNull List<String> ofPackage(final @NotNull DirectoryDto dir) {
        final @NotNull ArrayList<String> generatedFqcn = withoutTestCasesDir(dir.getPath2());

        if (generatedFqcn.isEmpty()) {
            generatedFqcn.add("generated");
        }

        generatedFqcn.replaceAll(NameSanitizer::packageName);
        return generatedFqcn;
    }

    /**
     * The tree path a Java name is built from. The test cases directory is a
     * place in the tree, not a package, so it never appears in generated code.
     */
    private static @NotNull ArrayList<String> withoutTestCasesDir(final @NotNull List<String> path2) {
        final @NotNull ArrayList<String> names = new ArrayList<>(path2);
        names.remove(DirectoryType.TCD.getDisplayedName());
        return names;
    }

    /**
     * The last name becomes the class and everything before it becomes packages.
     */
    private static void sanitizeTail(final @NotNull ArrayList<String> names) {
        final int lastIdx = names.size() - 1;
        names.set(lastIdx, NameSanitizer.className(names.get(lastIdx)));

        for (int i = 0; i < lastIdx; i++) {
            names.set(i, NameSanitizer.packageName(names.get(i)));
        }
    }
}
