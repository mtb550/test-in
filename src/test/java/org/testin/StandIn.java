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

package org.testin;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Proxy;

/**
 * An object of an interface nothing in the test asks anything of. It answers
 * the three questions every object answers, and refuses the rest by name, so a
 * test that starts relying on it fails saying which call it made.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StandIn {
    public static <T> @NotNull T of(final @NotNull Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "a stand-in " + type.getSimpleName();
            default -> throw new UnsupportedOperationException("A stand-in " + type.getSimpleName() + " was asked " + method.getName());
        }));
    }
}
