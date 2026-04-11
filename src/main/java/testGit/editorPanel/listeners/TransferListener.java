package testGit.editorPanel.listeners;

import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.BaseEditorUI;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        List<TestCaseDto> items = rawList.getSelectedValuesList().stream()
                .filter(TestCaseDto.class::isInstance)
                .map(TestCaseDto.class::cast)
                .collect(Collectors.toList());

        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{FLAVOR};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return FLAVOR.equals(flavor);
            }

            @Override
            public @NotNull Object getTransferData(DataFlavor flavor) {
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
            Object data = support.getTransferable().getTransferData(FLAVOR);
            if (!(data instanceof List<?> rawList)) return false;

            List<TestCaseDto> items = rawList.stream()
                    .filter(TestCaseDto.class::isInstance)
                    .map(TestCaseDto.class::cast)
                    .toList();

            if (items.isEmpty()) return false;

            JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
            int insertAtLocal = dl.getIndex();
            int insertAtGlobal = (ui.getCurrentPage() - 1) * ui.getPageSize() + insertAtLocal;

            int[] globalDraggedIndices = new int[draggedIndices.length];
            for (int i = 0; i < draggedIndices.length; i++) {
                globalDraggedIndices[i] = (ui.getCurrentPage() - 1) * ui.getPageSize() + draggedIndices[i];
            }

            List<TestCaseDto> allItems = ui.getAllTestCaseDtos();
            List<TestCaseDto> itemsToMove = new ArrayList<>();

            synchronized (allItems) {
                int shift = 0;
                for (int globalDraggedIndex : globalDraggedIndices) {
                    if (globalDraggedIndex < insertAtGlobal) shift++;
                }
                insertAtGlobal -= shift;

                for (int i = globalDraggedIndices.length - 1; i >= 0; i--) {
                    itemsToMove.addFirst(allItems.remove(globalDraggedIndices[i]));
                }

                allItems.addAll(insertAtGlobal, itemsToMove);
            }

            ui.updateSequenceAndSaveAll();

            if (!itemsToMove.isEmpty()) {
                ui.selectTestCase(itemsToMove.getFirst());
            } else {
                ui.refreshView();
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
    }
}