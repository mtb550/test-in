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

package org.testin.codegen;

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

/**
 * UC-CODEGEN-002, Rule-CODEGEN-078.
 * <p>
 * A test case that has just been pasted as a copy, and the case it was copied
 * from.
 * <p>
 * The sibling of {@link MovedCase}, and about the same two places: a generator
 * is handed one object, and a copy is a new method here written from a method
 * over there. The difference is what happens to the original - a move takes the
 * method away, a copy leaves it exactly as it is - so the two are separate
 * operations rather than one with a flag.
 * <p>
 * <b>The original is the indexed case, not the one off the clipboard.</b> A
 * test case's parent is not written to its file and so is not on the clipboard
 * either, and the parent is the only way to the class holding its method. So the
 * paste looks the original up by id and hands the answer over, which also
 * settles the case that has been deleted since it was copied: it is not in the
 * index, there is no body to carry, and the copy gets the empty method a new
 * case gets.
 *
 * @param copy     the new test case, already parented to the test set it was pasted into
 * @param original the case it was copied from, parented to the test set that holds its method
 */
public record CopiedCase(@NotNull TestCaseDto copy, @NotNull TestCaseDto original) {
}
