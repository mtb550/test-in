package org.testin.java.codegen;

import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeGenerators;
import org.testin.codegen.GenAction;
import org.testin.codegen.GenType;
import org.testin.codegen.NoJavaCode;
import org.testin.java.codegen.clazz.CreateJavaClass;
import org.testin.java.codegen.clazz.RemoveJavaClass;
import org.testin.java.codegen.clazz.MoveJavaClass;
import org.testin.java.codegen.clazz.RenameJavaClass;
import org.testin.java.codegen.method.CreateTestMethod;
import org.testin.java.codegen.method.RemoveTestMethod;
import org.testin.java.codegen.method.update.UpdateTestDescription;
import org.testin.java.codegen.method.update.UpdateTestGroup;
import org.testin.java.codegen.method.update.UpdateTestOrder;
import org.testin.java.codegen.pkg.MoveJavaPackage;
import org.testin.java.codegen.pkg.RemoveJavaPackage;
import org.testin.java.codegen.pkg.RenameJavaPackage;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maps each generator type to the Java implementation of it.
 * <p>
 * This is the only bridge from the core into the Java plugin's PSI classes, and
 * it is now on the far side of a content-module boundary: the core reaches it
 * through {@link CodeGenerators}, which is empty in an IDE without the Java
 * plugin. Before that boundary existed the whole map sat in the core jar, where
 * the Plugin Verifier reported every class it names as unresolved against
 * PyCharm, GoLand and WebStorm - and the Marketplace published that on the
 * plugin page (#144).
 * <p>
 * The lazy class-loading this used to rely on is gone with it. It was correct -
 * OptionalPlugin.JAVA guarded every path here - but "correct at runtime" is not
 * something static analysis can see, which was the whole problem.
 */
@NoArgsConstructor
public final class GenRegistry implements CodeGenerators {

    private static final @NotNull Map<GenType, GenAction> ACTIONS = new EnumMap<>(GenType.class);

    static {
        ACTIONS.put(GenType.RENAME_TEST_PROJECT, new RenameJavaPackage());
        ACTIONS.put(GenType.REMOVE_TEST_PROJECT, new RemoveJavaPackage());
        ACTIONS.put(GenType.REMOVE_TEST_SET_PACKAGE, new RemoveJavaPackage());
        ACTIONS.put(GenType.RENAME_TEST_SET_PACKAGE, new RenameJavaPackage());
        ACTIONS.put(GenType.MOVE_TEST_SET_PACKAGE, new MoveJavaPackage());
        ACTIONS.put(GenType.CREATE_TEST_SET, new CreateJavaClass());
        ACTIONS.put(GenType.REMOVE_TEST_SET, new RemoveJavaClass());
        ACTIONS.put(GenType.RENAME_TEST_SET, new RenameJavaClass());
        ACTIONS.put(GenType.MOVE_TEST_SET, new MoveJavaClass());
        ACTIONS.put(GenType.CREATE_TEST_CASE, new CreateTestMethod());
        ACTIONS.put(GenType.REMOVE_TEST_CASE, new RemoveTestMethod());
        ACTIONS.put(GenType.UPDATE_TEST_CASE_DESCRIPTION, new UpdateTestDescription());
        ACTIONS.put(GenType.UPDATE_TEST_CASE_GROUP, new UpdateTestGroup());
        ACTIONS.put(GenType.UPDATE_TEST_CASE_ORDER, new UpdateTestOrder());
    }

    /**
     * The Java generator for this operation.
     * <p>
     * An operation with no Java behind it answers with {@link NoJavaCode} rather
     * than throwing: this is asked for every node kind now, including the ones
     * that were always data-only, and "generates nothing" is a real answer that
     * the type already knows how to give.
     */
    @Override
    public @NotNull GenAction actionFor(final @NotNull GenType type) {
        return Optional.ofNullable(ACTIONS.get(type))
                .orElseGet(() -> new NoJavaCode(type.getDescription()));
    }
}
