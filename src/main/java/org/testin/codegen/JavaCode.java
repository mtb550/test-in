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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;

@Getter
@AllArgsConstructor
public enum JavaCode {
    TP(
            new NoJavaCode(DirectoryType.TP.getDescription()),
            (p, renamed) -> GenType.RENAME_TEST_PROJECT.getAction().execute(p, renamed),
            new NoJavaCode(DirectoryType.TP.getDescription())
    ),

    TCD(
            new NoJavaCode(DirectoryType.TCD.getDescription()),
            new NoJavaCode(DirectoryType.TCD.getDescription()),
            new NoJavaCode(DirectoryType.TCD.getDescription())
    ),

    TRD(
            new NoJavaCode(DirectoryType.TRD.getDescription()),
            new NoJavaCode(DirectoryType.TRD.getDescription()),
            new NoJavaCode(DirectoryType.TRD.getDescription())
    ),

    TSP(
            new NoJavaCode(DirectoryType.TSP.getDescription()),
            (p, renamed) -> GenType.RENAME_TEST_SET_PACKAGE.getAction().execute(p, renamed),
            (p, moved) -> GenType.MOVE_TEST_SET_PACKAGE.getAction().execute(p, moved)
    ),

    TRP(
            new NoJavaCode(DirectoryType.TRP.getDescription()),
            new NoJavaCode(DirectoryType.TRP.getDescription()),
            new NoJavaCode(DirectoryType.TRP.getDescription())
    ),

    TS(
            (p, dir) -> GenType.CREATE_TEST_SET.getAction().execute(p, dir),
            (p, renamed) -> GenType.RENAME_TEST_SET.getAction().execute(p, renamed),
            (p, moved) -> GenType.MOVE_TEST_SET.getAction().execute(p, moved)
    ),

    TR(
            new NoJavaCode(DirectoryType.TR.getDescription()),
            new NoJavaCode(DirectoryType.TR.getDescription()),
            new NoJavaCode(DirectoryType.TR.getDescription())
    );

    // UC-CODEGEN-004, Rule-CODEGEN-023
    private final @NotNull GenAction created;

    // UC-CODEGEN-015, UC-CODEGEN-017, Rule-CODEGEN-051
    private final @NotNull GenAction renamed;

    // UC-CODEGEN-016, UC-CODEGEN-017, Rule-CODEGEN-053
    private final @NotNull GenAction moved;

    public static @NotNull JavaCode of(final @NotNull DirectoryType type) {
        return valueOf(type.name());
    }
}
