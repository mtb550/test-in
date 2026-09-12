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
import org.testin.model.dto.TestCaseDto;

@FunctionalInterface
public interface RevertAction {

    /**
     * The revert of a change there is no reverting: creating or deleting a whole
     * test case has no field to put back, and a run's results are work rather
     * than an edit.
     * <p>
     * A value rather than a null on {@link ChangeType}, so "cannot be reverted"
     * is stated by the type. What decides whether the row offers a revert at all
     * is {@link ChangeType#isRevertable()}, which is the question the dialog
     * actually asks.
     */
    RevertAction NONE = (currentDto, oldDto) -> {
    };

    void apply(final @NotNull TestCaseDto currentDto, final @NotNull TestCaseDto oldDto);
}
