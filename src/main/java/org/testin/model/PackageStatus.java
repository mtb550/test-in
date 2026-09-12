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
 * Whether a package still holds current work. Persisted in the test set package
 * and test run package markers, which share the meaning and so share the enum.
 * <p>
 * Archived keeps everything inside it and changes only how it is treated: the
 * package is retired ({@code DirectoryDto.isRetired()}) — drawn gray, ordered
 * after the active ones, left collapsed by expand-all, and its contents not
 * offered for a new run — so last quarter's runs stop being the first thing in
 * the tree (#68).
 */
@Getter
@AllArgsConstructor
public enum PackageStatus implements NodeStatus {
    ACTIVE(
            Bundle.message("status.package.active"),
            Bundle.message("status.package.active.action"),
            Bundle.message("status.package.active.description"),
            true
    ),

    ARCHIVED(
            Bundle.message("status.package.archived"),
            Bundle.message("status.package.archived.action"),
            Bundle.message("status.package.archived.description"),
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
