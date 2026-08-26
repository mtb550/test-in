package org.testin.codegen;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

/**
 * Whoever can turn a Testin node into automation code.
 * <p>
 * The core knows every operation there is - {@link GenType} lists them - and
 * nothing about how any of them is carried out. Writing a class and a method
 * means {@code PsiClass}, {@code PsiMethod} and {@code JavaPsiFacade}, which
 * only the IntelliJ Java plugin has, so the doing lives in the
 * {@code testin-java} content module and arrives here (#144).
 * <p>
 * <b>One per language, not one for all of them.</b> A second module generating
 * Robot Framework for PyCharm contributes to this same point; the core never
 * learns that a second language exists, and nothing here branches on which one
 * answered.
 */
public interface CodeGenerators {

    /**
     * Contributed by a content module, which loads only where its language
     * plugin does. Everywhere else the list is empty - an answer rather than a
     * missing one, which is why nothing here returns null.
     */
    @NotNull ExtensionPointName<CodeGenerators> EP = ExtensionPointName.create("org.testin.codeGenerators");

    /**
     * What this contributor does for that operation.
     */
    @NotNull GenAction actionFor(final @NotNull GenType type);

    /**
     * The generator for this operation, and one that quietly writes nothing when
     * no module claimed it.
     * <p>
     * {@link NoJavaCode} is the empty answer, the same value the node types that
     * never produce code already carry - so a caller in an IDE without the Java
     * plugin runs the same unconditional line as one in IntelliJ IDEA.
     */
    static @NotNull GenAction find(final @NotNull GenType type) {
        return EP.getExtensionList().stream()
                .findFirst()
                .map(generators -> generators.actionFor(type))
                .orElseGet(() -> new NoJavaCode(type.getDescription()));
    }
}
