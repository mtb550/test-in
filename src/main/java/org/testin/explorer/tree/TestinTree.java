package org.testin.explorer.tree;

import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.ui.treeStructure.SimpleTree;
import org.testin.actions.TestinData;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreeModel;

/**
 * The project tree, answering the platform's questions as well as its own
 * (#119).
 * <p>
 * An action declared in {@code plugin.xml} is built by the platform with a
 * no-arg constructor, so it cannot be handed a tree the way every action in this
 * plugin is handed one today. It asks instead, and this is what answers - which
 * is the whole reason declaring an action was never as small as adding a line of
 * XML: nothing here published a data key at all.
 * <p>
 * A class rather than an anonymous subclass at the construction site, because
 * {@code SimpleTree} does not implement {@link UiDataProvider} and the interface
 * has to be declared somewhere a reader can see it. Named for the convention the
 * plugin already follows where a plain noun would collide with a platform type -
 * {@code TestinEditor} against the platform's {@code Editor}.
 * <p>
 * It holds nothing and decides nothing. What is selected is read from the tree
 * itself, on the EDT, at the moment the platform asks.
 */
public class TestinTree extends SimpleTree implements UiDataProvider {

    public TestinTree(final @NotNull TreeModel model) {
        super(model);
    }

    // UC-INTERNAL-001, Rule-INTERNAL-002
    @Override
    public void uiDataSnapshot(final @NotNull DataSink sink) {
        TestinData.from(sink, this, TreeValueUtil.selectedDirectories(getSelectionPaths()));
    }
}
