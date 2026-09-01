package org.testin.editor.list;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.AbstractEditorContextMenu;
import org.testin.editor.TestinEditor;
import org.testin.editor.listeners.CardMouseListener;
import org.testin.editor.listeners.GridListSelectionSynchronizer;
import org.testin.editor.listeners.SelectionListener;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.util.FontSync;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Builds the card-list view used by both editors — the list-view counterpart of
 * {@code grid/GridPanelBuilder}. The editors keep only their own specifics
 * (renderer, drag-and-drop reordering, model sync).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ListPanelBuilder {

    public static @NotNull ListView build(final @NotNull Project p, final @NotNull Disposable fontSyncDisposable) {
        final @NotNull CollectionListModel<TestCaseDto> model = new CollectionListModel<>(new ArrayList<>());

        final @NotNull JBList<TestCaseDto> list = new JBList<>(model);
        list.setBackground(UIUtil.getPanelBackground());
        list.setOpaque(true);
        list.setPaintBusy(true);
        list.getEmptyText().setText("Loading...");
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setExpandableItemsEnabled(false);

        FontSync.syncWithNativeEditor(p, list, fontSyncDisposable);

        // A narrower list wraps a title over more lines, so the rows are taller -
        // and a JList in its default vertical orientation never finds that out.
        // BasicListUI recomputes cell heights when the model, the font or the
        // fixed height changes, and on a width change only for the two wrapping
        // orientations; for this one it takes the width change and does nothing
        // with it. So the rows are re-measured here, on the width alone: dragging
        // the editor's height changes no title's wrapping and is not worth a
        // full relayout of the page.
        list.addComponentListener(new ComponentAdapter() {
            private int lastWidth = -1;

            @Override
            public void componentResized(final @NotNull ComponentEvent e) {
                if (list.getWidth() == lastWidth) return;

                lastWidth = list.getWidth();
                model.allContentsChanged();
            }
        });

        final @NotNull JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(UIUtil.getPanelBackground());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        return new ListView(model, list, scrollPane);
    }

    /**
     * Wires the interaction listeners shared by the test and run editors:
     * mouse handling (hover icons, wheel forwarding, context menu), shortcuts,
     * view-panel selection sync, and grid-selection sync.
     */
    public static void wireCommonListeners(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull ListView view, final @NotNull DirectoryDto dir, final @NotNull AbstractEditorContextMenu contextMenu, final @NotNull Supplier<Optional<JBTable>> gridTableSupplier, final @NotNull BooleanSupplier gridActiveSupplier) {
        final @NotNull JBList<TestCaseDto> list = view.list();

        final @NotNull CardMouseListener mouseListener = new CardMouseListener(p, editor, list, view.model(), dir, contextMenu);
        list.addMouseListener(mouseListener);
        list.addMouseWheelListener(mouseListener);
        list.addMouseMotionListener(mouseListener);

        contextMenu.registerShortcuts(list, contextMenu);

        list.addListSelectionListener(new SelectionListener(p, list, editor, dir.getPath2()));
        list.addListSelectionListener(new GridListSelectionSynchronizer(list, gridTableSupplier, gridActiveSupplier));
    }
}
