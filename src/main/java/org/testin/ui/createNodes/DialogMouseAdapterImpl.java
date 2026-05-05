package org.testin.ui.createNodes;

import com.intellij.ui.components.JBList;
import org.testin.pojo.DirectoryType;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DialogMouseAdapterImpl extends MouseAdapter {
    private final JBList<DirectoryType> list;
    private final Runnable onSubmit;

    public DialogMouseAdapterImpl(final JBList<DirectoryType> list, final Runnable onSubmit) {
        this.list = list;
        this.onSubmit = onSubmit;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int clickedIndex = list.locationToIndex(e.getPoint());
        if (clickedIndex >= 0)
            onSubmit.run();
    }

}