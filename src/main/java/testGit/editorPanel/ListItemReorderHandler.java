package testGit.editorPanel;

import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.ImageUtil;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public class ListItemReorderHandler extends TransferHandler {
    private static final DataFlavor TESTCASE_LIST_FLAVOR = new DataFlavor(List.class, "List of TestCase");

    private final CollectionListModel<TestCase> model;
    private int[] draggedIndices;
    private List<TestCase> draggedItems;

    public ListItemReorderHandler(CollectionListModel<TestCase> model) {
        System.out.println("ListItemReorderHandler.ListItemReorderHandler()");
        this.model = model;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        System.out.println("ListItemReorderHandler.createTransferable()");

        @SuppressWarnings("unchecked")
        JBList<TestCase> list = (JBList<TestCase>) c;
        draggedIndices = list.getSelectedIndices();
        draggedItems = list.getSelectedValuesList();

        // --- create a ghost image of the first dragged card ---
        if (!draggedItems.isEmpty()) {
            TestCase tc = draggedItems.get(0);
            Component renderer = list.getCellRenderer()
                    .getListCellRendererComponent(list, tc, draggedIndices[0], true, false);
            Rectangle cellBounds = list.getCellBounds(draggedIndices[0], draggedIndices[0]);
            renderer.setSize(cellBounds.getSize());
            BufferedImage img = ImageUtil.createImage(
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
                System.out.println("ListItemReorderHandler.getTransferDataFlavors()");
                return new DataFlavor[]{TESTCASE_LIST_FLAVOR};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                System.out.println("ListItemReorderHandler.isDataFlavorSupported()");
                return TESTCASE_LIST_FLAVOR.equals(flavor);
            }

            @Override
            public @NotNull Object getTransferData(DataFlavor flavor)
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
        //System.out.println("ListItemReorderHandler.getSourceActions()");
        return MOVE;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        System.out.println("ListItemReorderHandler.canImport()");
        return support.isDrop()
                && support.isDataFlavorSupported(TESTCASE_LIST_FLAVOR);
    }

    @Override
    public boolean importData(TransferSupport support) {
        System.out.println("ListItemReorderHandler.importData()");
        if (!canImport(support)) return false;

        try {
            @SuppressWarnings("unchecked")
            List<TestCase> dropped = (List<TestCase>) support.getTransferable()
                    .getTransferData(TESTCASE_LIST_FLAVOR);
            JBList.DropLocation dl = (JBList.DropLocation) support.getDropLocation();
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
            ex.printStackTrace(System.out);
            return false;
        } finally {
            // clean up
            draggedIndices = null;
            draggedItems = null;
        }
    }
}
