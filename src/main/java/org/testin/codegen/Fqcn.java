package org.testin.codegen;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
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
     * Packages and class for a directory, or empty when there is no class to
     * name.
     * <p>
     * Empty and returned empty, because the message says nothing was built: this
     * used to notify and then fall into {@code get(-1)} on the next line, so the
     * tester read "no class name could be built" and got an
     * IndexOutOfBoundsException to go with it. Callers skip on empty (#66, F1).
     */
    public static @NotNull List<String> ofClass(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        final @NotNull ArrayList<String> generatedFqcn = withoutTestCasesDir(dir.getPath2());

        if (generatedFqcn.isEmpty()) {
            Services.getInstance(p, Notifier.class).softRefuse(p, "Class Name Unknown",
                    "'" + dir.getName() + "' sits outside a test cases directory, so no automation class name could be built.");
            return List.of();
        }

        sanitizeTail(generatedFqcn);
        return generatedFqcn;
    }

    /**
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
