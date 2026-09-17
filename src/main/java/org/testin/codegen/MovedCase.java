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
import org.testin.model.dto.dirs.DirectoryDto;

/**
 * UC-CODEGEN-002, Rule-CODEGEN-077.
 * <p>
 * A test case that has just moved to another test set, and the set it came
 * from.
 * <p>
 * The companion of {@link Moved}, for the same reason: a generator is handed one
 * object, and a move is about two places. The case carries where it is going -
 * its parent is the destination by the time this is built - and the set it left
 * is the only way to find the method, because a method is found through the
 * class of the set that holds the case.
 * <p>
 * The other way round from {@link Moved}, which is built <b>before</b> its node
 * moves. A test case is written into its new set first - that write is what
 * keeps the case's audit and its identity - so by the time the code can be
 * moved the data already has, and the old place has to be carried rather than
 * read.
 * <p>
 * Without it a cut and paste left the method in the old class: Run and Go to
 * code looked in the new one and found nothing, and removing the old test set
 * later deleted that class with the body the tester had written in it (#312,
 * A54).
 *
 * @param tc   the case, already parented to the test set it moved into
 * @param from the test set it was cut from, which still holds its method
 */
public record MovedCase(@NotNull TestCaseDto tc, @NotNull DirectoryDto from) {
}
