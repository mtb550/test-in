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

package org.testin.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * The words a failure can be reported with.
 * <p>
 * One owner because the question has one answer and it was being written out
 * at each place that reported a failure - four times correctly and six times
 * not at all. {@code getMessage()} is null for an exception thrown without one,
 * and every place that says a failure hands the words to a parameter declared
 * {@code @NotNull}, which the IDE instruments into a runtime check. So a push or
 * a sync that failed with no message made the handler reporting it throw, and
 * the tester was told nothing (#66, finding 183).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FailureText {

    /**
     * Its message, or - when it carries none - its type, which is still more
     * than nothing to go on.
     */
    public static @NotNull String of(final @NotNull Throwable failure) {
        return Objects.requireNonNullElse(failure.getMessage(), failure.toString());
    }
}
