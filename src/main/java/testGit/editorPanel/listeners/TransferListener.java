package testGit.editorPanel.listeners;

import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.BaseEditorUI;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransferListener extends TransferHandler {
    private static final DataFlavor FLAVOR = new DataFlavor(List.class, "List of TestCase");
    private final BaseEditorUI ui;
    private int[] draggedIndices;

    public TransferListener(final BaseEditorUI ui) {
        this.ui = ui;
    }

    @Override
    public int getSourceActions(final JComponent c) {
        return MOVE;
    }

    @Override
    protected Transferable createTransferable(final JComponent c) {
        if (!(c instanceof JList<?> rawList)) return null;

        draggedIndices = rawList.getSelectedIndices();

        final List<TestCaseDto> items = rawList.getSelectedValuesList().stream()
                .filter(TestCaseDto.class::isInstance)
                .map(TestCaseDto.class::cast)
                .toList();

        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
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

            final JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
            final int offset = (ui.getCurrentPage() - 1) * ui.getPageSize();
            int insertAtGlobal = offset + dl.getIndex();

            final int[] globalDraggedIndices = Arrays.stream(draggedIndices)
                    .map(i -> offset + i)
                    .toArray();

            final List<TestCaseDto> allItems = ui.getAllTestCaseDtos();
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

            ui.updateSequenceAndSaveAll();

            itemsToMove.stream().findFirst().ifPresentOrElse(
                    ui::selectTestCase,
                    ui::refreshView
            );

            return true;
        } catch (final Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
    }
}