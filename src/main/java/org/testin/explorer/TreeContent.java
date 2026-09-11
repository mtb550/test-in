package org.testin.explorer;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanelWithEmptyText;
import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;

/**
 * The component the tree tool window shows, and the one thing it answers about
 * itself: which project it belongs to.
 * <p>
 * <b>Why a panel has to answer that.</b> The platform builds the data context
 * for a tool window's title-bar buttons from the tool window's content, and in
 * the new UI it reads the project out of that context for <i>every</i> title
 * action, before the action runs - {@code event.project!!} in
 * {@code ToolWindowManagerAppLevelHelper}. A context that cannot answer is a
 * NullPointerException thrown by the platform, and the button the tester
 * pressed never runs.
 * <p>
 * <b>How it came up.</b> The panel is emptied and redrawn on every refresh, and
 * the welcome screen adds no children at all - it is drawn with the empty text.
 * So while a test project was bound, the tree inside it kept the context
 * answerable; the moment the project was archived and the panel redrew to the
 * welcome screen, the tool window held a component with nothing in it, and the
 * next press on Settings threw before it opened anything.
 * <p>
 * Answered here rather than by whatever happens to be inside: what the tool
 * window is for does not change with what it is currently showing, and a
 * welcome screen belongs to the same project a tree does (#66).
 * <p>
 * A named class rather than an anonymous subclass, for the reason
 * {@code TestinTree} is one: the interface has to be somewhere a reader can
 * see it.
 */
public class TreeContent extends JBPanelWithEmptyText implements UiDataProvider {

    private final @NotNull Project p;

    public TreeContent(final @NotNull Project p) {
        super(new BorderLayout());
        this.p = p;
    }

    // UC-TREE-PANEL-001, Rule-INTERNAL-002
    @Override
    public void uiDataSnapshot(final @NotNull DataSink sink) {
        sink.set(CommonDataKeys.PROJECT, p);
    }
}
