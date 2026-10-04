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

package org.testin.testng;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PreparedTest {

    private static @NotNull Module aModule(final @NotNull String name) {
        return (Module) Proxy.newProxyInstance(Module.class.getClassLoader(), new Class<?>[]{Module.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getName" -> name;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            default -> throw new UnsupportedOperationException("a stand-in module was asked " + method.getName());
        });
    }

    private static @NotNull Prepared across(final @NotNull List<Module> modules) {
        return new Prepared(List.of(), List.of(), modules);
    }

    // Rule-CODEGEN-097
    @Test
    public void methodsInOneModuleAreOneExecution() {
        final @NotNull Module app = aModule("app");

        assertFalse(across(List.of(app)).spansModules(), "one module was taken for several");
        assertFalse(across(List.of(app, app)).spansModules(), "two test methods in one module were taken for two modules");
        assertFalse(across(List.of()).spansModules(), "a selection with no module was taken for several");
    }

    // Rule-CODEGEN-097
    @Test
    public void methodsInTwoModulesSpanModules() {
        assertTrue(across(List.of(aModule("app"), aModule("api"))).spansModules(), "test methods in two modules were taken for one execution");
    }
}
