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

/**
 * What makes a node of each kind: one constant per {@link DirectoryType}, named
 * after it.
 * <p>
 * A column on that enum until #111, which is how the vocabulary package came to
 * import this one: a kind of node is a fact about the domain, and what happens
 * when a tester creates one is a fact about this feature. Two enums, each
 * declaring what its own package knows.
 * <p>
 * <b>Named, not looked up.</b> {@link #of} asks for the constant of the same
 * name, so nothing branches on the type and no call site asks what it is
 * holding - and nothing here can be reached by a kind of node that has no
 * creator, because there is no such kind. {@code NodeKindTablesTest} is what
 * says the two lists still match, which is the day an eighth kind arrives.
 * <p>
 * Three kinds refuse rather than create: a test project and the two fixed
 * containers are not made from the tree, and {@link NotCreatableFromTree} says
 * so out loud rather than leaving a caller to find there is no creator. What it
 * says is the kind's own word, asked of {@link DirectoryType}, because a second
 * spelling of "Test Cases directory" is one of them going stale.
 */
@AllArgsConstructor
public enum NodeCreators {
    TP(p -> new NotCreatableFromTree(DirectoryType.TP.getDescription())),
    TCD(p -> new NotCreatableFromTree(DirectoryType.TCD.getDescription())),
    TRD(p -> new NotCreatableFromTree(DirectoryType.TRD.getDescription())),
    TSP(CreateTestSetPackage::new),
    TRP(CreateTestRunPackage::new),
    TS(CreateTestSet::new),
    TR(CreateTestRun::new);

    private final @NotNull Function<Project, NodeCreator> creator;

    // UC-TREE-PANEL-002, Rule-TREE-PANEL-011
    public static @NotNull NodeCreator of(final @NotNull Project p, final @NotNull DirectoryType type) {
        return valueOf(type.name()).creator.apply(p);
    }
}
