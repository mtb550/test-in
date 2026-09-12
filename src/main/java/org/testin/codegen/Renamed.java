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
import org.testin.model.dto.dirs.DirectoryDto;

/**
 * A node and the name it is about to take.
 * <p>
 * A {@link GenAction} is handed one object, and a rename needs two things: the
 * node as it still is, so its generated code can be found where it currently
 * sits, and the name it is becoming. That is why renaming used to go around
 * {@link GenType} and call the generators directly, behind an instanceof chain
 * of its own (#51).
 * <p>
 * The node has not been renamed yet when this is built. The order matters: the
 * Java is renamed first, while the old name is still what finds it.
 */
public record Renamed(@NotNull DirectoryDto dir, @NotNull String newName) {
}
