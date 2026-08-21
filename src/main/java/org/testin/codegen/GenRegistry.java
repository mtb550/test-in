package org.testin.codegen;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import java.util.Optional;
import org.testin.codegen.clazz.CreateJavaClass;
import org.testin.codegen.clazz.RemoveJavaClass;
import org.testin.codegen.clazz.MoveJavaClass;
import org.testin.codegen.clazz.RenameJavaClass;
import org.testin.codegen.method.CreateTestMethod;
import org.testin.codegen.method.RemoveTestMethod;
import org.testin.codegen.method.RenameTestMethod;
import org.testin.codegen.method.update.UpdateTestDescription;
import org.testin.codegen.method.update.UpdateTestGroup;
import org.testin.codegen.method.update.UpdateTestPriority;
import org.testin.codegen.pkg.MoveJavaPackage;
import org.testin.codegen.pkg.RemoveJavaPackage;
import org.testin.codegen.pkg.RenameJavaPackage;

import java.util.EnumMap;
import java.util.Map;

/**
 * Maps each Java-backed generator type to its implementation. This is the only
 * bridge from core code into the Java-plugin (PSI) classes, and it is
 * class-loaded exclusively behind {@code OptionalPlugin.JAVA.isAvailable()}
 * (see {@link GenType#getAction()}), so IDEs without the Java plugin
 * never touch the PSI-dependent classes.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class GenRegistry {

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
        ACTIONS.put(GenType.RENAME_TEST_CASE, new RenameTestMethod());
        ACTIONS.put(GenType.UPDATE_TEST_CASE_DESCRIPTION, new UpdateTestDescription());
        ACTIONS.put(GenType.UPDATE_TEST_CASE_GROUP, new UpdateTestGroup());
        ACTIONS.put(GenType.UPDATE_TEST_CASE_PRIORITY, new UpdateTestPriority());
    }

    static @NotNull GenAction actionFor(final @NotNull GenType type) {
        return Optional.ofNullable(ACTIONS.get(type))
                .orElseThrow(() -> new IllegalStateException("No generator registered for " + type));
    }
}
