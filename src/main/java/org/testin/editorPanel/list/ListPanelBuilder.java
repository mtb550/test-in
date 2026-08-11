package org.testin.editorPanel.list;

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
import org.testin.editorPanel.AbstractEditorContextMenu;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.listeners.GridListSelectionSynchronizer;
import org.testin.editorPanel.listeners.MouseListenerImpl;
import org.testin.editorPanel.listeners.SelectionListener;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.util.FontSync;

import javax.swing.*;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Builds the card-list view used by both editors — the list-view counterpart of
 * {@code grid/GridPanelBuilder}. The editors keep only their own specifics
 * (renderer, drag-and-drop reordering, model sync).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ListPanelBuilder {

    public static ListView build(final @NotNull Project p, final @NotNull Disposable fontSyncDisposable) {
        final CollectionListModel<TestCaseDto> model = new CollectionListModel<>(new ArrayList<>());

        final JBList<TestCaseDto> list = new JBList<>(model);
        list.setBackground(UIUtil.getPanelBackground());
        list.setOpaque(true);
        list.setPaintBusy(true);
        list.getEmptyText().setText("Loading...");
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setExpandableItemsEnabled(false);

        FontSync.syncWithNativeEditor(p, list, fontSyncDisposable);

        final JBScrollPane scrollPane = new JBScrollPane(list);
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
    public static void wireCommonListeners(final @NotNull Project p,
                                           final @NotNull IEditor editor,
                                           final @NotNull ListView view,
                                           final @NotNull DirectoryDto dir,
                                           final @NotNull AbstractEditorContextMenu contextMenu,
                                           final @NotNull Supplier<JBTable> gridTableSupplier,
                                           final @NotNull BooleanSupplier gridActiveSupplier) {
        final JBList<TestCaseDto> list = view.list();

        final MouseListenerImpl mouseListener = new MouseListenerImpl(p, editor, list, view.model(), dir, contextMenu);
        list.addMouseListener(mouseListener);
        list.addMouseWheelListener(mouseListener);
        list.addMouseMotionListener(mouseListener);

        contextMenu.registerShortcuts(list, contextMenu);

        list.addListSelectionListener(new SelectionListener(p, list, editor, dir.getPath2()));
        list.addListSelectionListener(new GridListSelectionSynchronizer(list, gridTableSupplier, gridActiveSupplier));
    }
}
