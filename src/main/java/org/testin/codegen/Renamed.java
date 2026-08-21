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
