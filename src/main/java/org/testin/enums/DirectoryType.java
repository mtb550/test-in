package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenAction;
import org.testin.codegen.GenType;
import org.testin.codegen.NoJavaCode;
import org.testin.creator.*;
import org.testin.indexer.ProjectIndexer;
import org.testin.services.Services;

import javax.swing.*;
import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum DirectoryType {
    TP(
            "Test Project",
            "",
            AllIcons.Nodes.Project,
            ".tp",
            p -> new NotCreatableFromTree("Test Project"),
            (p, dir) -> GenType.CREATE_TEST_SET_PACKAGE.getAction().execute(p, dir),
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestProject(dir.getPath(), removed -> {
                if (removed) GenType.REMOVE_TEST_PROJECT.getAction().execute(p, dir);
                onRemoved.accept(removed);
            }),
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
    ),

    TCD(
            "Test Cases Directory",
            "Test Cases",
            AllIcons.Nodes.Bookmark,
            ".tcd",
            p -> new NotCreatableFromTree("Test Cases directory"),
            new NoJavaCode("Test Cases directory"),
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeFixedContainer(dir.getPath(), onRemoved),
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
    ),

    TRD(
            "Test Runs Directory",
            "Test Runs",
            AllIcons.Nodes.Bookmark,
            ".trd",
            p -> new NotCreatableFromTree("Test Runs directory"),
            new NoJavaCode("Test Runs directory"),
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeFixedContainer(dir.getPath(), onRemoved),
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
    ),

    TSP(
            "Test Set Package",
            "",
            AllIcons.Nodes.WebFolder,
            ".tsp",
            CreateTestSetPackage::new,
            (p, dir) -> GenType.CREATE_TEST_SET_PACKAGE.getAction().execute(p, dir),
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestSetPackage(dir.getPath(), removed -> {
                if (removed) GenType.REMOVE_TEST_SET_PACKAGE.getAction().execute(p, dir);
                onRemoved.accept(removed);
            }),
            SimpleTextAttributes.REGULAR_ATTRIBUTES
    ),

    TRP(
            "Test Run Package",
            "",
            AllIcons.Nodes.WebFolder,
            ".trp",
            CreateTestRunPackage::new,
            new NoJavaCode("test run package"),
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestRunPackage(dir.getPath(), onRemoved),
            SimpleTextAttributes.REGULAR_ATTRIBUTES
    ),

    TS(
            "Test Set",
            "",
            AllIcons.Vcs.Changelist,
            ".ts",
            CreateTestSet::new,
            (p, dir) -> GenType.CREATE_TEST_SET.getAction().execute(p, dir),
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestSet(dir.getPath(), removed -> {
                if (removed) GenType.REMOVE_TEST_SET.getAction().execute(p, dir);
                onRemoved.accept(removed);
            }),
            SimpleTextAttributes.REGULAR_ATTRIBUTES
    ),

    TR(
            "Test Run",
            "",
            AllIcons.Toolwindows.ToolWindowRunWithCoverage,
            ".tr",
            CreateTestRun::new,
            new NoJavaCode("test run"),
            (p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestRun(dir.getPath(), onRemoved),
            SimpleTextAttributes.REGULAR_ATTRIBUTES
    );

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

    private final @NotNull RemoveHandler removeHandler;
    private final @NotNull SimpleTextAttributes attributes;

}
