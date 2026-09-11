package org.testin.model;

import org.jetbrains.annotations.NotNull;

/**
 * What a node is, as far as the tree is concerned: active, archived,
 * deprecated. Three enums answer it - {@link ProjectStatus} for a test project,
 * {@link TestSetStatus} for a test set, {@link PackageStatus} for a package -
 * and this is the part they have in common, so the menu entry that sets one and
 * the Details popup that shows one never ask which kind of node they are
 * holding (#110).
 * <p>
 * The three getters are the three the enums already had. Implementing this
 * costs each of them one word and no method: Lombok generates them from the
 * fields, the way {@link org.testin.model.markers.Marker} is implemented.
 */
public interface NodeStatus {

    /**
     * The status of a node that has none - a test cases directory, a runs
     * directory.
     * <p>
     * Empty strings rather than null, so a reader asking a marker what its
     * status is gets an answer from every marker and nothing has to ask whether
     * this one has a status. It is what makes
     * {@link org.testin.model.markers.Marker#getStatusLabel()} one line: the
     * popup drops a blank row already.
     */
    @NotNull NodeStatus NONE = new NodeStatus() {

        @Override
        public @NotNull String getLabel() {
            return "";
        }

        @Override
        public @NotNull String getButtonName() {
            return "";
        }

        @Override
        public @NotNull String getButtonDescription() {
            return "";
        }

        @Override
        public boolean isActive() {
            return true;
        }
    };

    /**
     * UC-TREE-PANEL-001, Rule-TREE-PANEL-099.
     * <p>
     * Whether the node is in current work, which is what it is until somebody
     * says otherwise.
     * <p>
     * <b>The tree draws a status beside a name only when this is false.</b>
     * "Active" on every test project, every test set and every package is a
     * word a tester reads a hundred times and needed never; "Inactive",
     * "Deprecated" and "Archived" are the ones worth the space.
     * <p>
     * Declared per constant rather than worked out from the name or the
     * declaration order, so a status added to any of the three enums says for
     * itself which kind it is.
     */
    boolean isActive();

    /**
     * What the node is, as the tester reads it: "Archived", "Deprecated".
     */
    @NotNull String getLabel();

    /**
     * The menu entry that sets it: "Archive", "Mark Deprecated".
     */
    @NotNull String getButtonName();

    /**
     * What that entry does, for the tooltip and Find Action.
     */
    @NotNull String getButtonDescription();
}
