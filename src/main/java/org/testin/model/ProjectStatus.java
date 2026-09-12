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

package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

/**
 * What a test project is, as far as the tree is concerned: the one being worked
 * on, or not.
 * <p>
 * <b>Two, and no more.</b> There was an ARCHIVED beside these until #66, which
 * meant the same thing INACTIVE means - put aside - and asked the tester to tell
 * two kinds of "not now" apart. A project is either what this repository is
 * about or it is not, and one word for that is one word to learn.
 * <p>
 * There is no removed state either, and deliberately: removing a test project
 * deletes its directory and the automation package with it, so there is nothing
 * left to carry a status. A REMOVED constant was declared here and never
 * assigned by anything - kept for a while in case a marker already on disk held
 * it, and no marker ever did. Its siblings {@link TestSetStatus} and
 * {@link PackageStatus} have never had one.
 * <p>
 * A marker still saying ARCHIVED does not parse, so it is read with the default
 * and reported as damaged - test data is wiped rather than migrated here, and a
 * converter for one renamed word would be the first of them.
 */
@Getter
@AllArgsConstructor
public enum ProjectStatus implements NodeStatus {
    ACTIVE(
            Bundle.message("status.project.active"),
            Bundle.message("status.project.active.action"),
            Bundle.message("status.project.active.description"),
            true
    ),

    INACTIVE(
            Bundle.message("status.project.inactive"),
            Bundle.message("status.project.inactive.action"),
            Bundle.message("status.project.inactive.description"),
            false
    );

    private final @NotNull String label;
    private final @NotNull String buttonName;
    private final @NotNull String buttonDescription;

    /**
     * See {@link NodeStatus#isActive()} - the tree says nothing beside a node
     * that is in current work.
     */
    private final boolean active;
}
