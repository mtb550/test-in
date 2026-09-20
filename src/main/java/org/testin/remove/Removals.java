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

package org.testin.remove;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.DirectoryType;
import org.testin.model.RemoveHandler;
import org.testin.services.Services;

@AllArgsConstructor
public enum Removals {
    TP((p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestProject(dir.getPath(), removed -> {
        if (removed) GenType.REMOVE_TEST_PROJECT.getAction().execute(p, dir);
        onRemoved.accept(removed);
    })),

    TCD(
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).refuseRemove(dir.getPath(), onRemoved)
    ),

    TRD(
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).refuseRemove(dir.getPath(), onRemoved)
    ),

    TSP((p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestSetPackage(dir.getPath(), removed -> {
        if (removed) GenType.REMOVE_TEST_SET_PACKAGE.getAction().execute(p, dir);
        onRemoved.accept(removed);
    })),

    TRP(
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestRunPackage(dir.getPath(), onRemoved)
    ),

    TS((p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestSet(dir.getPath(), removed -> {
        if (removed) GenType.REMOVE_TEST_SET.getAction().execute(p, dir);
        onRemoved.accept(removed);
    })),

    TR(
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestRun(dir.getPath(), onRemoved)
    );

    private final @NotNull RemoveHandler handler;

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-046
    public static @NotNull RemoveHandler of(final @NotNull DirectoryType type) {
        return valueOf(type.name()).handler;
    }
}
