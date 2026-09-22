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

package org.testin.importexport.imports;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@FunctionalInterface
public interface ImportSetter {
    static @NotNull ImportSetter always(final @NotNull BiConsumer<TestCaseDto, String> write) {
        return (p, tc, value) -> {
            write.accept(tc, value);
            return true;
        };
    }

    static <T> boolean took(final @NotNull Optional<T> read, final @NotNull Consumer<T> onto) {
        read.ifPresent(onto);

        return read.isPresent();
    }

    boolean execute(final @NotNull Project p, final @NotNull TestCaseDto tc, final @NotNull String value);
}
