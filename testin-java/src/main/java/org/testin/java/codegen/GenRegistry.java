/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.java.codegen;

import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.CodeGenerators;
import org.testin.codegen.GenAction;
import org.testin.codegen.GenType;
import org.testin.codegen.NoJavaCode;
import org.testin.java.codegen.clazz.CreateJavaClass;
import org.testin.java.codegen.clazz.MoveJavaClass;
import org.testin.java.codegen.clazz.RemoveJavaClass;
import org.testin.java.codegen.clazz.RenameJavaClass;
import org.testin.java.codegen.method.CopyTestMethod;
import org.testin.java.codegen.method.CreateTestMethod;
import org.testin.java.codegen.method.MoveTestMethod;
import org.testin.java.codegen.method.RemoveTestMethod;
import org.testin.java.codegen.method.update.ReconcileTestMethod;
import org.testin.java.codegen.method.update.UpdateTestDescription;
import org.testin.java.codegen.method.update.UpdateTestEnabled;
import org.testin.java.codegen.method.update.UpdateTestGroup;
import org.testin.java.codegen.method.update.UpdateTestOrder;
import org.testin.java.codegen.pkg.MoveJavaPackage;
import org.testin.java.codegen.pkg.RemoveJavaPackage;
import org.testin.java.codegen.pkg.RenameJavaPackage;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor
public final class GenRegistry implements CodeGenerators {
    private final @NotNull Map<GenType, GenAction> actions = byType();

    private static @NotNull Map<GenType, GenAction> byType() {
        final @NotNull Map<GenType, GenAction> byType = new EnumMap<>(GenType.class);

        byType.put(GenType.RENAME_TEST_PROJECT, new RenameJavaPackage());
        byType.put(GenType.REMOVE_TEST_PROJECT, new RemoveJavaPackage());
        byType.put(GenType.REMOVE_TEST_SET_PACKAGE, new RemoveJavaPackage());
        byType.put(GenType.RENAME_TEST_SET_PACKAGE, new RenameJavaPackage());
        byType.put(GenType.MOVE_TEST_SET_PACKAGE, new MoveJavaPackage());
        byType.put(GenType.CREATE_TEST_SET, new CreateJavaClass());
        byType.put(GenType.REMOVE_TEST_SET, new RemoveJavaClass());
        byType.put(GenType.RENAME_TEST_SET, new RenameJavaClass());
        byType.put(GenType.MOVE_TEST_SET, new MoveJavaClass());
        byType.put(GenType.CREATE_TEST_CASE, new CreateTestMethod());
        byType.put(GenType.REMOVE_TEST_CASE, new RemoveTestMethod());
        byType.put(GenType.MOVE_TEST_CASE, new MoveTestMethod());
        byType.put(GenType.COPY_TEST_CASE, new CopyTestMethod());
        byType.put(GenType.UPDATE_TEST_CASE_DESCRIPTION, new UpdateTestDescription());
        byType.put(GenType.UPDATE_TEST_CASE_GROUP, new UpdateTestGroup());
        byType.put(GenType.UPDATE_TEST_CASE_ORDER, new UpdateTestOrder());
        byType.put(GenType.UPDATE_TEST_CASE_STATUS, new UpdateTestEnabled());
        byType.put(GenType.RECONCILE_TEST_CASE, new ReconcileTestMethod());

        return byType;
    }

    @Override
    public @NotNull GenAction actionFor(final @NotNull GenType type) {
        return Optional.ofNullable(actions.get(type))
                .orElseGet(() -> new NoJavaCode(type.getDescription()));
    }
}
