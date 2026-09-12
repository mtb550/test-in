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

package org.testin.model.markers;

import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeStatus;
import org.testin.model.PackageStatus;

import java.util.List;

/**
 * A marker that carries a {@link PackageStatus}: the test set package and the
 * test run package. The two markers are separate classes because they are
 * separate files with separate names on disk, but archiving means the same
 * thing to both, so one action sets it through this and never asks which
 * package it is holding.
 */
// UnusedReturnValue reports setStatus for the same reason it reports Marker's
// setters, and the answer is the same one: see the comment on Marker (#66).
@SuppressWarnings("UnusedReturnValue")
public interface PackageMarker extends Marker {

    @NotNull PackageStatus getStatus();

    PackageMarker setStatus(@NotNull PackageStatus status);

    @Override
    default @NotNull NodeStatus status() {
        return getStatus();
    }

    @Override
    default @NotNull List<NodeStatus> statuses() {
        return List.of(PackageStatus.values());
    }

    @Override
    default void applyStatus(final @NotNull NodeStatus status) {
        setStatus((PackageStatus) status);
    }
}
