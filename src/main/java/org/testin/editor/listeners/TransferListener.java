package org.testin.editor.listeners;

import org.testin.notifications.Done;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editor.TestinEditor;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransferListener extends TransferHandler {
    private static final @NotNull DataFlavor FLAVOR = new DataFlavor(List.class, "List of TestCase");
    private final @NotNull Project p;
    private final @NotNull TestinEditor editor;
    /**
     * The rows the drag started on, and empty when it did not start on this list.
     */
    private int @NotNull [] draggedIndices = new int[0];

    public TransferListener(final @NotNull Project p, final @NotNull TestinEditor editor) {
        this.p = p;
        this.editor = editor;
    }

    @Override
    public int getSourceActions(final JComponent c) {
        return MOVE;
    }

    @Override
    protected @Nullable Transferable createTransferable(final JComponent c) {
        if (!(c instanceof JBList<?> rawList)) return null;

        draggedIndices = rawList.getSelectedIndices();

        final @NotNull List<TestCaseDto> items = rawList.getSelectedValuesList().stream()
                .filter(TestCaseDto.class::isInstance)
                .map(TestCaseDto.class::cast)
                .toList();

        return new Transferable() {
            @Override
            public DataFlavor @NotNull [] getTransferDataFlavors() {
                return new DataFlavor[]{FLAVOR};
            }

            @Override
            public boolean isDataFlavorSupported(final DataFlavor flavor) {
                return FLAVOR.equals(flavor);
            }

            @Override
            public @NotNull Object getTransferData(final DataFlavor flavor) {
                return items;
            }
        };
    }

    @Override
    public boolean canImport(final TransferSupport support) {
        return support.isDataFlavorSupported(FLAVOR);
    }

    @Override
    public boolean importData(final TransferSupport support) {
        // Asked before the transferable is, because getTransferData throws for a
        // flavor it does not carry - and the exception's message is the flavor's
        // own name, so the log read "Exception: List of TestCase" and said
        // nothing about a drag that was simply not ours to take.
        if (!support.isDataFlavorSupported(FLAVOR)) return false;

        try {
            final @NotNull Object data = support.getTransferable().getTransferData(FLAVOR);
            if (!(data instanceof List<?> rawList)) return false;

            final @NotNull List<TestCaseDto> items = rawList.stream()
                    .filter(TestCaseDto.class::isInstance)
                    .map(TestCaseDto.class::cast)
                    .toList();

            if (items.isEmpty()) return false;

            // Empty when the drag did not start on this list: there is nothing
            // here to reorder, and treating it as an insert would duplicate.
            final int[] dragged = draggedIndices;
            if (dragged.length == 0) return false;

            final @NotNull JBList.DropLocation dl = (JBList.DropLocation) support.getDropLocation();
            final int offset = editor.globalIndex(0);
            int insertAtGlobal = offset + dl.getIndex();

            final int[] globalDraggedIndices = Arrays.stream(dragged)
                    .map(i -> offset + i)
                    .toArray();

            final @NotNull List<TestCaseDto> allItems = editor.getAllTestCases();
            final @NotNull List<TestCaseDto> itemsToMove = new ArrayList<>();

            synchronized (allItems) {
                int finalInsertAtGlobal = insertAtGlobal;
                final int shift = (int) Arrays.stream(globalDraggedIndices)
                        .filter(idx -> idx < finalInsertAtGlobal)
                        .count();

                insertAtGlobal -= shift;

                for (int i = globalDraggedIndices.length - 1; i >= 0; i--) {
                    itemsToMove.addFirst(allItems.remove(globalDraggedIndices[i]));
                }

                allItems.addAll(insertAtGlobal, itemsToMove);
            }

            editor.updateSequenceAndSaveAll();

            // After the save, inside the try: a drop that threw on the way here
            // is logged, not confirmed (#62).
            Services.getInstance(p, Notifier.class).softShowCounted(p, Done.RE_SORTED, itemsToMove.size());

            itemsToMove.stream().findFirst().ifPresentOrElse(
                    editor::selectTestCase,
                    editor::refreshView
            );

            return true;
        } catch (final Exception ex) {
            Logger.error("Reordering the test cases failed: " + ex.getMessage());
            return false;
        }
    }
}