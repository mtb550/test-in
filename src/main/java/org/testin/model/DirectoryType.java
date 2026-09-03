package org.testin.model;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenAction;
import org.testin.codegen.GenType;
import org.testin.codegen.Moved;
import org.testin.codegen.NoJavaCode;
import org.testin.codegen.Renamed;
import org.testin.creator.*;
import org.testin.indexer.ProjectIndexer;
import org.testin.services.Services;

import javax.swing.*;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum DirectoryType {
    TP(
            "Test Project",
            "",
            AllIcons.Nodes.Project,
            ".tp",
            p -> new NotCreatableFromTree("Test Project"),
            new NoJavaCode("a test project on its own"),
            (p, renamed) -> GenType.RENAME_TEST_PROJECT.getAction().execute(p, renamed),
            new NoJavaCode("A test project never moves; it"),
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestProject(dir.getPath(), removed -> {
                if (removed) GenType.REMOVE_TEST_PROJECT.getAction().execute(p, dir);
                onRemoved.accept(removed);
            }),
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            List.of(NodeCount.TEST_SETS, NodeCount.PACKAGES, NodeCount.TEST_CASES, NodeCount.TEST_RUNS)
    ),

    TCD(
            "Test Cases Directory",
            "Test Cases",
            AllIcons.Nodes.Bookmark,
            ".tcd",
            p -> new NotCreatableFromTree("Test Cases directory"),
            new NoJavaCode("Test Cases directory"),
            new NoJavaCode("Test Cases directory"),
            new NoJavaCode("Test Cases directory"),
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).refuseRemove(dir.getPath(), onRemoved),
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            List.of(NodeCount.TEST_SETS, NodeCount.PACKAGES, NodeCount.TEST_CASES)
    ),

    TRD(
            "Test Runs Directory",
            "Test Runs",
            AllIcons.Nodes.Bookmark,
            ".trd",
            p -> new NotCreatableFromTree("Test Runs directory"),
            new NoJavaCode("Test Runs directory"),
            new NoJavaCode("Test Runs directory"),
            new NoJavaCode("Test Runs directory"),
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).refuseRemove(dir.getPath(), onRemoved),
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            List.of(NodeCount.PACKAGES, NodeCount.TEST_RUNS)
    ),

    TSP(
            "Test Set Package",
            "",
            AllIcons.Nodes.WebFolder,
            ".tsp",
            CreateTestSetPackage::new,
            new NoJavaCode("a test set package on its own"),
            (p, renamed) -> GenType.RENAME_TEST_SET_PACKAGE.getAction().execute(p, renamed),
            (p, moved) -> GenType.MOVE_TEST_SET_PACKAGE.getAction().execute(p, moved),
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestSetPackage(dir.getPath(), removed -> {
                if (removed) GenType.REMOVE_TEST_SET_PACKAGE.getAction().execute(p, dir);
                onRemoved.accept(removed);
            }),
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            List.of(NodeCount.TEST_SETS, NodeCount.PACKAGES, NodeCount.TEST_CASES)
    ),

    TRP(
            "Test Run Package",
            "",
            AllIcons.Nodes.WebFolder,
            ".trp",
            CreateTestRunPackage::new,
            new NoJavaCode("test run package"),
            new NoJavaCode("test run package"),
            new NoJavaCode("test run package"),
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestRunPackage(dir.getPath(), onRemoved),
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            List.of(NodeCount.PACKAGES, NodeCount.TEST_RUNS)
    ),

    TS(
            "Test Set",
            "",
            AllIcons.Vcs.Changelist,
            ".ts",
            CreateTestSet::new,
            (p, dir) -> GenType.CREATE_TEST_SET.getAction().execute(p, dir),
            (p, renamed) -> GenType.RENAME_TEST_SET.getAction().execute(p, renamed),
            (p, moved) -> GenType.MOVE_TEST_SET.getAction().execute(p, moved),
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestSet(dir.getPath(), removed -> {
                if (removed) GenType.REMOVE_TEST_SET.getAction().execute(p, dir);
                onRemoved.accept(removed);
            }),
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            List.of(NodeCount.TEST_CASES)
    ),

    TR(
            "Test Run",
            "",
            AllIcons.Toolwindows.ToolWindowRunWithCoverage,
            ".tr",
            CreateTestRun::new,
            new NoJavaCode("test run"),
            new NoJavaCode("test run"),
            new NoJavaCode("test run"),
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestRun(dir.getPath(), onRemoved),
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            NodeStatistics.VERDICTS,
            List.of(NodeCount.TOTAL)
    );

    /**
     * What a directory directly under Test Cases may be marked as, and in which
     * order to ask.
     * <p>
     * The order is the precedence, and it was written out at four call sites and
     * written down at none: a directory carrying both markers is read as a test
     * set, because a set is what holds cases and a package is what holds sets.
     * Anyone probing in the other order would have got a different tree and no
     * warning about it (#173).
     */
    public static final @NotNull List<DirectoryType> UNDER_TEST_CASES = List.of(TS, TSP);

    /**
     * The same question on the run side, with the same precedence rule.
     */
    public static final @NotNull List<DirectoryType> UNDER_TEST_RUNS = List.of(TR, TRP);

    /**
     * The markers a family is recognized by, joined the way a warning says them -
     * {@code .ts/.tsp}. Asked rather than typed, so a marker renamed here does not
     * leave a log line describing the name it used to have.
     */
    public static @NotNull String markerNames(final @NotNull List<DirectoryType> family) {
        return family.stream().map(DirectoryType::getMarker).collect(Collectors.joining("/"));
    }

    /**
     * What this kind is called in a log line - the description in lower case, so
     * the word and its capitalized form cannot drift apart. Every reader of a
     * marker used to be handed this word by hand, beside the marker's file name
     * and the class it parses to: three facts about one thing, spelled out at
     * seven call sites, and nothing checking they belonged together (#173).
     */
    public @NotNull String getMarkerKind() {
        return description.toLowerCase(Locale.ROOT);
    }

    private final @NotNull String description;

    /**
     * The fixed label the node shows instead of its own name, empty when it
     * shows its own — the two containers are the only ones with a label.
     */
    private final @NotNull String displayedName;
    private final @NotNull Icon icon;
    private final @NotNull String marker;

    /**
     * Never null: a type the tree cannot create carries
     * {@link NotCreatableFromTree}, which says so rather than leaving the
     * caller to discover there is no creator at all.
     */
    private final @NotNull Function<Project, NodeCreator> action;

    /**
     * Never null: a type that produces no Java carries {@link NoJavaCode}, so
     * callers run it either way rather than testing whether one exists.
     */
    private final @NotNull GenAction codegen;

    /**
     * What a rename does to this kind of node's code, given a {@link Renamed}.
     * <p>
     * Here beside the create and remove hooks rather than in the rename action,
     * which used to ask {@code instanceof} which generator a node wanted - the
     * third place to answer a question this enum exists to answer once (#51).
     */
    private final @NotNull GenAction renameCodegen;

    /**
     * What a move does to this kind of node's code, given a {@link Moved}.
     * <p>
     * A move changes which package a file declares, so it is its own operation
     * and not a rename with a different argument. Nothing did it at all before:
     * a dragged test set left its class behind, and the cases under it stopped
     * being runnable (#51).
     */
    private final @NotNull GenAction moveCodegen;

    private final @NotNull RemoveHandler removeHandler;
    private final @NotNull SimpleTextAttributes attributes;

    /**
     * Which of the two ways of counting a node this kind is counted by, and so
     * what its Details draws - see {@link NodeStatistics}. Declared here beside
     * the icon and the codegen because it is the same kind of fact: something
     * true of the type, answered once, rather than a question the Details
     * dialog asks about the node in front of it (#82).
     */
    private final @NotNull NodeStatistics statistics;

    /**
     * The counts this kind of node reports as rows in its Details, in order.
     * <p>
     * A type lists only what can apply to it. Nothing on the test-case side can
     * hold a run and nothing on the run side can hold a test set, so those are
     * impossible states rather than choices, and a {@code 0} there would imply
     * it could be otherwise. A test run lists its total alone: the rest of its
     * numbers are the chart, and printing them twice would be the popup
     * disagreeing with itself about which one to read.
     */
    private final @NotNull List<NodeCount> counts;
}
