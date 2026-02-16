package testGit.editorPanel.testCaseEditor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
import testGit.pojo.TestCase;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class TransferImpl extends TransferHandler {
    private static final DataFlavor FLAVOR = new DataFlavor(List.class, "List of TestCase");
    private final CollectionListModel<TestCase> model;
    private final Directory dir;
    private final Runnable onDragStart; // Triggered when the user starts dragging
    private int[] draggedIndices;

    public TransferImpl(Directory dir, CollectionListModel<TestCase> model, Runnable onDragStart) {
        this.model = model;
        this.dir = dir;
        this.onDragStart = onDragStart;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        // 1. Reset filters immediately when drag begins
        if (onDragStart != null) {
            onDragStart.run();
        }

        JBList<TestCase> list = (JBList<TestCase>) c;
        // 2. Now that the list is unfiltered, these indices are globally correct
        draggedIndices = list.getSelectedIndices();
        List<TestCase> items = list.getSelectedValuesList();

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
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(FLAVOR);
    }

    @Override
    public boolean importData(TransferSupport support) {
        try {
            List<TestCase> items = (List<TestCase>) support.getTransferable().getTransferData(FLAVOR);
            JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
            int insertAt = dl.getIndex();

            // 1. Calculate how many dragged items are located BEFORE the drop index
            // This is crucial for "down to up" or "up to down" logic
            int shift = 0;
            for (int draggedIndex : draggedIndices) {
                if (draggedIndex < insertAt) {
                    shift++;
                }
            }

            // 2. Adjust the insertion point
            insertAt -= shift;

            // 3. Remove items using the saved indices (stored in createTransferable)
            for (int i = draggedIndices.length - 1; i >= 0; i--) {
                model.remove(draggedIndices[i]);
            }

            // 4. Insert items at the adjusted global index
            for (TestCase item : items) {
                model.add(insertAt++, item);
            }

            updateSequenceAndSave();
            return true;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    private void updateSequenceAndSave() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        for (int i = 0; i < model.getSize(); i++) {
            TestCase current = model.getElementAt(i);
            current.setIsHead(i == 0);
            current.setNext(i < model.getSize() - 1 ? UUID.fromString(model.getElementAt(i + 1).getId()) : null);

            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(new File(dir.getFile(), current.getId() + ".json"), current);
            } catch (IOException ignored) {
            }
        }
    }
}