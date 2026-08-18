package org.testin.model.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.DirectoryType;
import org.testin.model.markers.Marker;

import java.nio.file.Path;
import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString()
public abstract class DirectoryDto {
    @NonNull
    @Builder.Default
    private String name = "";

    @NonNull
    @Builder.Default
    private Path path = Path.of("");

    @NonNull
    @Builder.Default
    private ArrayList<String> path2 = new ArrayList<>();

    /**
     * Null for the root node: nothing above the test project.
     */
    @ToString.Exclude
    private @Nullable DirectoryDto parent;

    /**
     * The node's marker; each subtype's Lombok-generated getter returns its
     * concrete marker type and satisfies this by covariant return. Audit info
     * (created/modified by and when) lives only here — the marker JSON is the
     * persisted truth, the DTO stores none of it.
     */
    @NonNull
    public abstract Marker getMarker();

    /**
     * File name of this node's marker JSON inside the directory.
     */
    @NonNull
    public abstract String getMarkerFileName();


    // Capability flags replace the instanceof chains that used to branch on
    // node type across the actions (issue #37): a new node type declares what
    // it supports here instead of being hunted for at every call site.

    /**
     * True when nodes can be created under this one. The test project creates
     * its children itself, and a test set or run holds test cases rather than
     * nodes, so only the containers say yes.
     */
    public boolean canCreateChildren() {
        return false;
    }

    /**
     * True when the user may rename this node; the fixed root containers say no.
     */
    public boolean isRenamable() {
        return true;
    }

    /**
     * True when test cases can be imported into or exported from this node.
     */
    public boolean isTestCaseContainer() {
        return false;
    }

    /**
     * True when the node opens in an editor tab (test sets and test runs).
     */
    public boolean isOpenableInEditor() {
        return false;
    }

    /**
     * True when the node can be cut, copied or dragged to another location;
     * the test project and the fixed root containers say no.
     */
    public boolean isTransferable() {
        return true;
    }

    /**
     * True when the user may remove this node; the fixed root containers say no.
     * A test project may be removed, behind a confirmation that says how much
     * goes with it - it is the largest delete in the plugin and the undo service
     * deliberately does not record removals.
     */
    public boolean isRemovable() {
        return true;
    }

    /**
     * True when transferred nodes may be pasted or dropped into this node;
     * the test project says no.
     */
    public boolean isTransferTarget() {
        return true;
    }

    /**
     * True when this node may be pasted or dropped into the test-set family;
     * run nodes say no — they live only under the test-runs directory.
     */
    public boolean isAllowedInTestSetFamily() {
        return true;
    }

    /**
     * True when this node may be pasted or dropped into the run family;
     * test-set nodes say no — they live only under the test-cases directory.
     */
    public boolean isAllowedInTestRunFamily() {
        return true;
    }

    /**
     * True when this node may be pasted or dropped into a test run;
     * test runs and run packages say no — a run holds run items only,
     * never nested run structure.
     */
    public boolean isAllowedInsideTestRun() {
        return true;
    }

    /**
     * True when the given node may be pasted or dropped into this one; the
     * test-set family additionally rejects run nodes.
     */
    public boolean acceptsTransferred(final @NotNull DirectoryDto source) {
        return isTransferTarget();
    }

    /**
     * True when the node is out of current work: a deprecated test set, an
     * archived package. Nothing inside it changes; what changes is how the
     * plugin treats it - drawn gray, sorted after its siblings, left collapsed
     * by expand-all, and not offered when a run is configured. Declared here so
     * the renderer, the children index, the tree node and the run dialog ask
     * one question instead of each knowing which status enum means "retired"
     * on which node (#68).
     */
    public boolean isRetired() {
        return false;
    }

    @NotNull
    public abstract DirectoryType getType();

}
