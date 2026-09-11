package org.testin.codegen;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;

/**
 * What happens to a node kind's generated Java when the node is created,
 * renamed or moved: one constant per {@link DirectoryType}, named after it.
 * <p>
 * Three columns on that enum until #111, which named five classes from this
 * package and made the vocabulary import a side module. What a node <i>is</i>
 * belongs to the domain; what its code does when it moves belongs here, beside
 * the generators that do it.
 * <p>
 * Every kind answers all three, and the ones that generate nothing answer
 * {@link NoJavaCode} - a test run records what was executed and writes no Java.
 * Stated rather than left to a null, so every caller runs the answer
 * unconditionally.
 * <p>
 * {@link #of} asks for the constant of the same name; nothing branches on the
 * type, and {@code NodeKindTablesTest} says the two lists still match.
 */
@Getter
@AllArgsConstructor
public enum JavaCode {
    TP(
            new NoJavaCode("a test project on its own"),
            (p, renamed) -> GenType.RENAME_TEST_PROJECT.getAction().execute(p, renamed),
            new NoJavaCode("A test project never moves; it")
    ),

    TCD(
            new NoJavaCode("Test Cases directory"),
            new NoJavaCode("Test Cases directory"),
            new NoJavaCode("Test Cases directory")
    ),

    TRD(
            new NoJavaCode("Test Runs directory"),
            new NoJavaCode("Test Runs directory"),
            new NoJavaCode("Test Runs directory")
    ),

    TSP(
            new NoJavaCode("a test set package on its own"),
            (p, renamed) -> GenType.RENAME_TEST_SET_PACKAGE.getAction().execute(p, renamed),
            (p, moved) -> GenType.MOVE_TEST_SET_PACKAGE.getAction().execute(p, moved)
    ),

    TRP(
            new NoJavaCode("test run package"),
            new NoJavaCode("test run package"),
            new NoJavaCode("test run package")
    ),

    TS(
            (p, dir) -> GenType.CREATE_TEST_SET.getAction().execute(p, dir),
            (p, renamed) -> GenType.RENAME_TEST_SET.getAction().execute(p, renamed),
            (p, moved) -> GenType.MOVE_TEST_SET.getAction().execute(p, moved)
    ),

    TR(
            new NoJavaCode("test run"),
            new NoJavaCode("test run"),
            new NoJavaCode("test run")
    );

    /**
     * UC-CODEGEN-004, Rule-CODEGEN-023 - the class or method the node brings
     * into being.
     */
    private final @NotNull GenAction created;

    /**
     * UC-CODEGEN-011, Rule-CODEGEN-031 - what a rename does to it. Here rather
     * than in the rename action, which used to ask {@code instanceof} which
     * generator a node wanted (#51).
     */
    private final @NotNull GenAction renamed;

    /**
     * UC-CODEGEN-011, Rule-CODEGEN-033 - what a move does to it. A move changes
     * which package a file declares, so it is its own operation and not a
     * rename with a different argument. Nothing did it at all before: a dragged
     * test set left its class behind, and the cases under it stopped being
     * runnable (#51).
     */
    private final @NotNull GenAction moved;

    public static @NotNull JavaCode of(final @NotNull DirectoryType type) {
        return valueOf(type.name());
    }
}
