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

package org.testin.codegen;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class JavaCode {
    // UC-CODEGEN-004, Rule-CODEGEN-023
    private final @NotNull GenAction created;

    // UC-CODEGEN-015, UC-CODEGEN-017, Rule-CODEGEN-051
    private final @NotNull GenAction renamed;

    // UC-CODEGEN-016, UC-CODEGEN-017, Rule-CODEGEN-053
    private final @NotNull GenAction moved;

    public static @NotNull JavaCode of(final @NotNull DirectoryType type) {
        final @NotNull GenAction none = new NoJavaCode(type.getDescription());
        return switch (type) {
            case TP -> new JavaCode(
                    none,
                    (p, renamed) -> GenType.RENAME_TEST_PROJECT.getAction().execute(p, renamed),
                    none
            );

            case TCD, TRD, TRP, TR -> new JavaCode(
                    none,
                    none,
                    none
            );

            case TSP -> new JavaCode(
                    none,
                    (p, renamed) -> GenType.RENAME_TEST_SET_PACKAGE.getAction().execute(p, renamed),
                    (p, moved) -> GenType.MOVE_TEST_SET_PACKAGE.getAction().execute(p, moved)
            );

            case TS -> new JavaCode(
                    (p, dir) -> GenType.CREATE_TEST_SET.getAction().execute(p, dir),
                    (p, renamed) -> GenType.RENAME_TEST_SET.getAction().execute(p, renamed),
                    (p, moved) -> GenType.MOVE_TEST_SET.getAction().execute(p, moved)
            );
        };
    }
}
