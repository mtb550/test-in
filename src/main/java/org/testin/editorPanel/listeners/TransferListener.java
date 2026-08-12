package org.testin.editorPanel.listeners;

import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editorPanel.IEditor;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransferListener extends TransferHandler {
    private static final @NotNull DataFlavor FLAVOR = new DataFlavor(List.class, "List of TestCase");
    private final @NotNull IEditor editor;
    private int @Nullable [] draggedIndices;

    public TransferListener(final @NotNull IEditor editor) {
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

        final List<TestCaseDto> items = rawList.getSelectedValuesList().stream()
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
        try {
            final Object data = support.getTransferable().getTransferData(FLAVOR);
            if (!(data instanceof List<?> rawList)) return false;

            final List<TestCaseDto> items = rawList.stream()
                    .filter(TestCaseDto.class::isInstance)
                    .map(TestCaseDto.class::cast)
                    .toList();

            if (items.isEmpty()) return false;

            final JBList.DropLocation dl = (JBList.DropLocation) support.getDropLocation();
            final int offset = (editor.getCurrentPage() - 1) * editor.getPageSize();
            int insertAtGlobal = offset + dl.getIndex();

            final int[] globalDraggedIndices = Arrays.stream(draggedIndices)
                    .map(i -> offset + i)
                    .toArray();

            final List<TestCaseDto> allItems = editor.getAllTestCases();
            final List<TestCaseDto> itemsToMove = new ArrayList<>();

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

            itemsToMove.stream().findFirst().ifPresentOrElse(
                    editor::selectTestCase,
                    editor::refreshView
            );

            return true;
        } catch (final Exception ex) {
            Logger.error("Exception: " + ex.getMessage());
            return false;
        }
    }
}