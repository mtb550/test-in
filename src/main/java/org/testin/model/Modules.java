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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What a module is: free text a tester types, so what exists is what has been
 * used.
 * <p>
 * The twin of {@link Groups}, and here for the same reason. Both editors
 * gathered the modules of the cases in front of them with the same eight lines -
 * collect, trim, drop the blanks - and the group half of the same pair already
 * asked an owner for its answer two methods below it. Two copies of one rule is
 * one of them going stale: a module typed with a trailing space is the same
 * module, and the day that stops being true it has to stop being true in one
 * place (#291).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Modules {

    /**
     * Every module these test cases name, once each.
     * <p>
     * Trimmed, and a case that names no module adds nothing - a blank is the
     * absence of a module rather than a module called nothing, and offering it
     * in the filter would be a row the tester cannot use.
     */
    public static @NotNull Set<String> in(final @NotNull List<TestCaseDto> testCases) {
        final @NotNull Set<String> modules = new HashSet<>();

        for (final TestCaseDto tc : testCases) {
            final @NotNull String module = tc.getModule().trim();
            if (!module.isEmpty()) modules.add(module);
        }

        return modules;
    }
}
