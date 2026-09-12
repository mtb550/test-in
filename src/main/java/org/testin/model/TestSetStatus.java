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
 * Whether a test set is still current. Persisted in the test set marker.
 * <p>
 * Deprecated is not deleted: the cases stay readable and the runs that already
 * used them keep their history. What it changes is what happens next — a
 * deprecated set is retired ({@code DirectoryDto.isRetired()}): drawn gray,
 * ordered after its siblings, and not offered when a new run is configured (#68).
 */
@Getter
@AllArgsConstructor
public enum TestSetStatus implements NodeStatus {
    ACTIVE(
            Bundle.message("status.testset.active"),
            Bundle.message("status.testset.active.action"),
            Bundle.message("status.testset.active.description"),
            true
    ),

    DEPRECATED(
            Bundle.message("status.testset.deprecated"),
            Bundle.message("status.testset.deprecated.action"),
            Bundle.message("status.testset.deprecated.description"),
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
