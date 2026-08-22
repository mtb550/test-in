package org.testin.indexer;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.model.NodeFigures;
import org.testin.model.TestRunSummary;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.services.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Counts what a tree node holds, on the call, keeping nothing.
 * <p>
 * The two ways a node can be counted, and the whole of both: a container is the
 * sum of what is beneath it, and a test run is the verdicts it recorded. Which
 * of the two applies is {@link org.testin.model.NodeStatistics}'s declaration -
 * every type names one - so neither method here asks what it was given.
 * <p>
 * Nothing is stored and nothing is cached. Every read behind these is RAM: the
 * children come from the indexer's own index and the results from the run the
 * store already holds, so a count is correct the moment a sync brings in
 * someone else's test cases, for the same reason it costs nothing to take.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NodeCounter {

    /**
     * What lies beneath a container: its test sets, its packages, its test
     * cases and its test runs, at any depth.
     * <p>
     * Retired branches are counted. A deprecated test set still holds its
     * cases and a package's size is its size; that it is out of current work is
     * already visible in the tree, which draws it gray and sorts it last.
     * <p>
     * The two package kinds are summed rather than chosen between: nothing on
     * the test-case side can hold a run package and nothing on the run side can
     * hold a set package, so one of the two terms is always zero and asking
     * which would only re-derive what the tree already enforces.
     */
    public static @NotNull NodeFigures childCounts(final @NotNull Project p, final @NotNull DirectoryDto dto) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        final @NotNull List<DirectoryDto> beneath = beneath(indexer, dto);

        final @NotNull Map<DirectoryType, Long> byType = beneath.stream()
                .collect(Collectors.groupingBy(DirectoryDto::getType, Collectors.counting()));

        return NodeFigures.ofChildren(
                counted(byType, DirectoryType.TS),
                counted(byType, DirectoryType.TSP) + counted(byType, DirectoryType.TRP),
                indexer.caseCountOf(dto.getPath())
                        + beneath.stream().mapToLong(node -> indexer.caseCountOf(node.getPath())).sum(),
                counted(byType, DirectoryType.TR));
    }

    /**
     * How a run went, from {@link TestRunSummary} rather than from the results
     * again: the popup is its second caller, not its second implementation, so
     * a run's Details and its PDF cannot disagree about the same run.
     * <p>
     * A run directory the scan could not read anything out of counts as nothing
     * rather than failing: the tree already shows the node, and the Details it
     * opens should still say what the node is.
     */
    public static @NotNull NodeFigures runVerdicts(final @NotNull Project p, final @NotNull DirectoryDto dto) {
        return Services.getInstance(p, ProjectIndexer.class).findTestRun(dto.getPath())
                .map(run -> NodeFigures.ofRun(TestRunSummary.of(run.getResults())))
                .orElse(NodeFigures.NONE);
    }

    /**
     * Every node under this one, at any depth, in no particular order - it is
     * about to be tallied.
     */
    private static @NotNull List<DirectoryDto> beneath(final @NotNull ProjectIndexer indexer,
                                                       final @NotNull DirectoryDto node) {
        final @NotNull List<DirectoryDto> found = new ArrayList<>();

        for (final DirectoryDto child : indexer.getChildren(node.getPath())) {
            found.add(child);
            found.addAll(beneath(indexer, child));
        }

        return found;
    }

    private static long counted(final @NotNull Map<DirectoryType, Long> byType, final @NotNull DirectoryType type) {
        return byType.getOrDefault(type, 0L);
    }
}
