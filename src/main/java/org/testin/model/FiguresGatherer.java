package org.testin.model;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.DirectoryDto;

/**
 * How one kind of node arrives at its numbers.
 * <p>
 * Named rather than a bare function so {@link NodeStatistics} reads as a
 * declaration - the same reason {@code GenAction} and {@code RemoveHandler}
 * are interfaces and not {@code BiConsumer}s.
 */
@FunctionalInterface
public interface FiguresGatherer {

    @NotNull
    NodeFigures of(final @NotNull Project p, final @NotNull DirectoryDto dto);
}
