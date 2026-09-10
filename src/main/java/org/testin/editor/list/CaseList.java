package org.testin.editor.list;

import com.intellij.model.Pointer;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.platform.navbar.backend.NavBarItem;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.TestinData;
import org.testin.editor.TestinEditor;
import org.testin.navigate.TestinNavBarItem;
import org.testin.model.dto.TestCaseDto;

/**
 * The card list, answering the platform's questions as well as its own (#119).
 * <p>
 * An action declared in {@code plugin.xml} is built by the platform with a
 * no-arg constructor, so it cannot be handed this list or the editor around it.
 * It asks, and this is what answers - the same job {@code TestinTree} does for
 * the tree, and the reason both editors' actions can be declared at all.
 * <p>
 * It also carries the width answer the list has always needed, which was an
 * anonymous subclass here before this one existed:
 * <p>
 * UC-EDITOR-PANEL-001 and UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-003. A card is
 * drawn to the width it is given - a long title wraps onto more lines rather
 * than running off the side - so this list never scrolls sideways, and saying so
 * is what stops it trying.
 * <p>
 * JList works the answer out instead, by comparing the widest card with the
 * viewport, and until the list has been laid out it has no width to give:
 * {@code CardTitle.titleColumnWidth} reads zero, decides there is no column to
 * wrap inside, and lets the title run as far as it likes, which lays the card
 * out 32767 pixels wide. So opening an editor put a horizontal scrollbar under a
 * virtually endless row, and every re-measure as the real width arrived grew its
 * thumb a little until the bar went away.
 */
public class CaseList extends JBList<TestCaseDto> implements UiDataProvider {

    private final @NotNull TestinEditor editor;

    public CaseList(final @NotNull CollectionListModel<TestCaseDto> model, final @NotNull TestinEditor editor) {
        super(model);
        this.editor = editor;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194
    @Override
    public void uiDataSnapshot(final @NotNull DataSink sink) {
        TestinData.from(sink, editor, getSelectedValuesList());

        // UC-EDITOR-PANEL-024, Rule-EDITOR-PANEL-216.
        //
        // Where the IDE's navigation bar starts walking. It asks the focused
        // component for one item and then asks TestinNavBarItemProvider for the
        // node above, over and over, so this is the only place that has to say
        // which node the tester is looking at (#161).
        sink.set(NavBarItem.NAVBAR_ITEM_KEY, Pointer.hardPointer(new TestinNavBarItem(editor.getP(), editor.getParent())));
    }
}
