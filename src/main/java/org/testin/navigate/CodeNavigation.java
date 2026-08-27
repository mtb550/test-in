package org.testin.navigate;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.util.List;
import java.util.Optional;

/**
 * Jumping from a test case to the generated method that runs it.
 * <p>
 * An interface here and an implementation in the {@code testin-java} content
 * module, because finding a method means {@code JavaPsiFacade}, {@code PsiClass}
 * and {@code PsiMethod} - classes only the IntelliJ Java plugin has. Keeping
 * them in the core jar is what made the Plugin Verifier report them as
 * unresolved against PyCharm, GoLand and WebStorm, which the Marketplace then
 * publishes on the plugin page (#144).
 * <p>
 * The guard did not move: {@code OptionalPlugin.JAVA} still decides whether the
 * action is offered, and this decides what happens when it is.
 */
public interface CodeNavigation {

    /**
     * Contributed by the content module, which loads only where the Java plugin
     * does. Everywhere else the list is empty - which is an answer, not a
     * missing one, and is why nothing here returns null.
     */
    @NotNull ExtensionPointName<CodeNavigation> EP = ExtensionPointName.create("org.testin.codeNavigation");

    /**
     * Opens the generated method that runs this test case.
     * <p>
     * The case rather than a name worked out from it. Navigation used to be
     * handed the method name the description sanitizes to and match on it, with
     * a case-insensitive second pass when that missed - so a method a tester had
     * renamed by hand was unreachable from the gutter while the updaters kept
     * editing it happily, and two cases whose descriptions differ only in
     * punctuation both opened the one method written for the pair.
     */
    void toCode(final @NotNull Project p, final @NotNull TestCaseDto tc);

    /**
     * The class and method that actually carry this case's id, and empty when
     * no generated method does.
     * <p>
     * Here rather than worked out from the description, because a name built
     * from a description is not an identity: two cases whose descriptions differ
     * only in punctuation or capitals sanitize to the same method name, and only
     * one method is ever written for the pair.
     * <p>
     * On this interface because the answer needs {@code PsiClass} and
     * {@code PsiMethod}, which exist only where the Java plugin does - the same
     * reason {@code toCode} is here. Finding the code and opening it are the two
     * halves of one question: {@code toCode} does both, and the runner needs the
     * first half without the second.
     */
    @NotNull Optional<List<String>> methodOf(final @NotNull Project p, final @NotNull TestCaseDto tc);

    /**
     * Whoever can navigate here, and one that says it cannot when nobody can.
     * <p>
     * The empty case is a value of its own type rather than an absent one, so no
     * caller asks whether navigation exists before asking it to navigate - the
     * same reason {@code NoJavaCode} exists beside {@code GenAction}.
     */
    static @NotNull CodeNavigation available() {
        return EP.getExtensionList().stream()
                .findFirst()
                .orElseGet(NoCodeNavigation::new);
    }
}
