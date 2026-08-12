package org.testin.codegen;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.clazz.CreateJavaClass;
import org.testin.codegen.clazz.RemoveJavaClass;
import org.testin.codegen.clazz.RenameJavaClass;
import org.testin.codegen.method.CreateTestMethod;
import org.testin.codegen.method.RemoveTestMethod;
import org.testin.codegen.method.RenameTestMethod;
import org.testin.codegen.method.update.UpdateTestDescription;
import org.testin.codegen.method.update.UpdateTestGroup;
import org.testin.codegen.method.update.UpdateTestPriority;
import org.testin.codegen.pkg.CreateJavaPackage;
import org.testin.codegen.pkg.RemoveJavaPackage;
import org.testin.codegen.pkg.RenameJavaPackage;

import java.util.EnumMap;
import java.util.Map;

/**
 * Maps each Java-backed generator type to its implementation. This is the only
 * bridge from core code into the Java-plugin (PSI) classes, and it is
 * class-loaded exclusively behind {@code OptionalPlugin.JAVA.isAvailable()}
 * (see {@link GeneratorType#getAction()}), so IDEs without the Java plugin
 * never touch the PSI-dependent classes.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class GeneratorRegistry {

    private static final Map<GeneratorType, GeneratorAction> ACTIONS = new EnumMap<>(GeneratorType.class);

    static {
        ACTIONS.put(GeneratorType.CREATE_TEST_PROJECT, new CreateJavaPackage());
        ACTIONS.put(GeneratorType.REMOVE_TEST_PROJECT, new RemoveJavaPackage());
        ACTIONS.put(GeneratorType.RENAME_TEST_PROJECT, new RenameJavaPackage());
        ACTIONS.put(GeneratorType.CREATE_TEST_SET_PACKAGE, new CreateJavaPackage());
        ACTIONS.put(GeneratorType.REMOVE_TEST_SET_PACKAGE, new RemoveJavaPackage());
        ACTIONS.put(GeneratorType.RENAME_TEST_SET_PACKAGE, new RenameJavaPackage());
        ACTIONS.put(GeneratorType.CREATE_TEST_SET, new CreateJavaClass());
        ACTIONS.put(GeneratorType.REMOVE_TEST_SET, new RemoveJavaClass());
        ACTIONS.put(GeneratorType.RENAME_TEST_SET, new RenameJavaClass());
        ACTIONS.put(GeneratorType.CREATE_TEST_CASE, new CreateTestMethod());
        ACTIONS.put(GeneratorType.REMOVE_TEST_CASE, new RemoveTestMethod());
        ACTIONS.put(GeneratorType.RENAME_TEST_CASE, new RenameTestMethod());
        ACTIONS.put(GeneratorType.UPDATE_TEST_CASE_DESCRIPTION, new UpdateTestDescription());
        ACTIONS.put(GeneratorType.UPDATE_TEST_CASE_GROUP, new UpdateTestGroup());
        ACTIONS.put(GeneratorType.UPDATE_TEST_CASE_PRIORITY, new UpdateTestPriority());
    }

    static @NotNull GeneratorAction actionFor(final @NotNull GeneratorType type) {
        final GeneratorAction action = ACTIONS.get(type);
        if (action == null) throw new IllegalStateException("No generator registered for " + type);
        return action;
    }
}
