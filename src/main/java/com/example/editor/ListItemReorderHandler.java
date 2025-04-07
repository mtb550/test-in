package com.example.editor;

import com.example.pojo.TestCase;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public class ListItemReorderHandler extends TransferHandler {
    private static final DataFlavor TESTCASE_LIST_FLAVOR =
            new DataFlavor(List.class, "List of TestCase");

    private final DefaultListModel<TestCase> model;
    private int[] draggedIndices;
    private List<TestCase> draggedItems;

    public ListItemReorderHandler(DefaultListModel<TestCase> model) {
        this.model = model;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        @SuppressWarnings("unchecked")
        JList<TestCase> list = (JList<TestCase>) c;
        draggedIndices = list.getSelectedIndices();
        draggedItems = list.getSelectedValuesList();

        // --- create a ghost image of the first dragged card ---
        if (!draggedItems.isEmpty()) {
            TestCase tc = draggedItems.get(0);
            Component renderer = list.getCellRenderer()
                    .getListCellRendererComponent(list, tc, draggedIndices[0], true, false);
            Rectangle cellBounds = list.getCellBounds(draggedIndices[0], draggedIndices[0]);
            renderer.setSize(cellBounds.getSize());
            BufferedImage img = new BufferedImage(
                    renderer.getWidth(), renderer.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g2 = img.createGraphics();
            renderer.paint(g2);
            g2.dispose();
            setDragImage(img);
            setDragImageOffset(new Point(0, 0));
        }

        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{TESTCASE_LIST_FLAVOR};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return TESTCASE_LIST_FLAVOR.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor)
                    throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) {
                    throw new UnsupportedFlavorException(flavor);
                }
                return draggedItems;
            }
        };
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDrop()
                && support.isDataFlavorSupported(TESTCASE_LIST_FLAVOR);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) return false;

        try {
            @SuppressWarnings("unchecked")
            List<TestCase> dropped = (List<TestCase>) support.getTransferable()
                    .getTransferData(TESTCASE_LIST_FLAVOR);
            JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
            int index = dl.getIndex();

            // 1) remove originals (highest to lowest)
            for (int i = draggedIndices.length - 1; i >= 0; i--) {
                model.remove(draggedIndices[i]);
            }

            // 2) adjust drop index if items removed before it
            int removedBefore = 0;
            for (int idx : draggedIndices) {
                if (idx < index) removedBefore++;
            }
            int insertAt = index - removedBefore;

            // 3) insert in same order
            for (TestCase tc : dropped) {
                model.add(insertAt++, tc);
            }

            return true;

        } catch (UnsupportedFlavorException | IOException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            // clean up
            draggedIndices = null;
            draggedItems = null;
        }
    }
}
