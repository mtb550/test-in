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

package org.testin.testcase.update.bulk;

import org.jetbrains.annotations.NotNull;

/**
 * What a bulk edit did to one row: a value the tester typed, or nothing because
 * they never touched it.
 * <p>
 * Untouched is not the same as emptied, and the difference matters in both
 * directions. A value written back is trimmed on the way, so writing an
 * untouched row back would edit a row nobody edited - while a row the tester
 * deliberately cleared is a change to apply, when the section accepts a blank
 * one.
 * <p>
 * That difference used to be carried by a null in a list of strings, which said
 * nothing about which of the two it meant and left every reader to remember.
 *
 * @param value   what to write, and empty on a row that was not edited
 * @param changed whether the tester edited this row at all
 */
public record EditedValue(@NotNull String value, boolean changed) {

    /**
     * A row the tester left alone. Nothing is written for it.
     */
    public static final @NotNull EditedValue UNCHANGED = new EditedValue("", false);

    public static @NotNull EditedValue of(final @NotNull String value) {
        return new EditedValue(value, true);
    }
}
