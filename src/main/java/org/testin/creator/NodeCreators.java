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

package org.testin.creator;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;

import java.util.function.Function;

@AllArgsConstructor
public enum NodeCreators {
    TP(
            _ -> new NotCreatableFromTree(DirectoryType.TP.getDescription())
    ),

    TCD(
            _ -> new NotCreatableFromTree(DirectoryType.TCD.getDescription())
    ),

    TRD(
            _ -> new NotCreatableFromTree(DirectoryType.TRD.getDescription())
    ),

    TSP(
            CreateTestSetPackage::new
    ),

    TRP(
            CreateTestRunPackage::new
    ),

    TS(
            CreateTestSet::new
    ),

    TR(
            CreateTestRun::new
    );

    private final @NotNull Function<Project, NodeCreator> creator;

    // UC-TREE-PANEL-002, Rule-TREE-PANEL-011
    public static @NotNull NodeCreator of(final @NotNull Project p, final @NotNull DirectoryType type) {
        return valueOf(type.name()).creator.apply(p);
    }
}
