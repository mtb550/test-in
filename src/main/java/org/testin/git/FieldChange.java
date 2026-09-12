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

package org.testin.git;

import org.jetbrains.annotations.NotNull;

/**
 * A single changed field inside a {@link PendingChange}.
 * <p>
 * Both values are always there. A field the tester never filled in is {@code ""}
 * on that side, not null - every compared field on {@link org.testin.model.dto.TestCaseDto}
 * is {@code @NonNull} with an empty default, and an addition or a removal reports
 * the empty string for the side that does not exist. These used to be declared
 * nullable for an absence the model does not have, which pushed a null check onto
 * everything that renders a row.
 */
public record FieldChange(@NotNull String fieldName, @NotNull String oldValue, @NotNull String newValue, @NotNull ChangeType changeType) {
}
