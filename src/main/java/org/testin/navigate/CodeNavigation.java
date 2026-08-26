package org.testin.navigate;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.util.List;

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
     * Opens the generated method named by this fully qualified name, the last
     * element being the method and the rest the class.
     */
    void toCode(final @NotNull Project p, final @NotNull List<String> fqcn);

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
                .orElseGet(() -> (p, fqcn) -> Logger.debug(
                        "No code navigation in this IDE; nothing opened for " + String.join(".", fqcn)));
    }
}
