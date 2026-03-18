package testGit.editorPanel.testCaseEditor;

import com.intellij.ui.CollectionListModel;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.mappers.TestCase;
import testGit.pojo.tree.dirs.Directory;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TransferImpl extends TransferHandler {
    private static final DataFlavor FLAVOR = new DataFlavor(List.class, "List of TestCase");
    private final CollectionListModel<TestCase> model;
    private final Directory dir;
    private final Runnable onDragStart;
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
        if (onDragStart != null) {
            onDragStart.run();
        }

        if (!(c instanceof JList<?> rawList)) return null;

        draggedIndices = rawList.getSelectedIndices();

        List<TestCase> items = rawList.getSelectedValuesList().stream()
                .filter(TestCase.class::isInstance)
                .map(TestCase.class::cast)
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
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(FLAVOR);
    }

    @Override
    public boolean importData(TransferSupport support) {
        try {
            Object data = support.getTransferable().getTransferData(FLAVOR);

            if (!(data instanceof List<?> rawList)) {
                return false;
            }

            List<TestCase> items = rawList.stream()
                    .filter(TestCase.class::isInstance)
                    .map(TestCase.class::cast)
                    .toList();

            if (items.isEmpty()) return false;

            JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
            int insertAt = dl.getIndex();

            int shift = 0;
            for (int draggedIndex : draggedIndices) {
                if (draggedIndex < insertAt) {
                    shift++;
                }
            }

            insertAt -= shift;

            for (int i = draggedIndices.length - 1; i >= 0; i--) {
                model.remove(draggedIndices[i]);
            }

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
        for (int i = 0; i < model.getSize(); i++) {
            TestCase current = model.getElementAt(i);
            current.setIsHead(i == 0);
            current.setNext(i < model.getSize() - 1 ? UUID.fromString(model.getElementAt(i + 1).getId()) : null);

            try {
                Config.getMapper().writerWithDefaultPrettyPrinter().writeValue(new File(dir.getPath().toFile(), current.getId() + ".json"), current);
            } catch (IOException ignored) {
            }
        }
    }
}