package org.testin.remove;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.DirectoryType;
import org.testin.model.RemoveHandler;
import org.testin.services.Services;

/**
 * What removing each kind of node takes - the indexer call that deletes it and
 * the generated Java that goes with it - as one constant per
 * {@link DirectoryType}, named after it.
 * <p>
 * A column on that enum until #111, which made the vocabulary package import
 * the indexer, the service locator and the code generator at once.
 * <p>
 * Here rather than in {@code indexer}: four of the seven removals also delete
 * generated code, and the indexer sits below {@code codegen} rather than above
 * it. This package is a gesture, so it may call both - and it is the only
 * caller.
 * <p>
 * Two of the seven refuse. The Test Cases and Test Runs directories are fixed
 * containers, and a refusal is a removal that answers false rather than a
 * missing entry - so {@code RemoveAction} counts completions without asking
 * which kind it just tried.
 */
@AllArgsConstructor
public enum Removals {
    TP((p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestProject(dir.getPath(), removed -> {
        if (removed) GenType.REMOVE_TEST_PROJECT.getAction().execute(p, dir);
        onRemoved.accept(removed);
    })),

    TCD((p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).refuseRemove(dir.getPath(), onRemoved)),

    TRD((p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).refuseRemove(dir.getPath(), onRemoved)),

    TSP((p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestSetPackage(dir.getPath(), removed -> {
        if (removed) GenType.REMOVE_TEST_SET_PACKAGE.getAction().execute(p, dir);
        onRemoved.accept(removed);
    })),

    TRP((p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestRunPackage(dir.getPath(), onRemoved)),

    TS((p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestSet(dir.getPath(), removed -> {
        if (removed) GenType.REMOVE_TEST_SET.getAction().execute(p, dir);
        onRemoved.accept(removed);
    })),

    TR((p, dir, onRemoved) -> Services.getInstance(p, ProjectIndexer.class).removeTestRun(dir.getPath(), onRemoved));

    private final @NotNull RemoveHandler handler;

    // UC-TREE-PANEL-013, Rule-TREE-PANEL-046
    public static @NotNull RemoveHandler of(final @NotNull DirectoryType type) {
        return valueOf(type.name()).handler;
    }
}
