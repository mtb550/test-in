package org.testin.ui.createNodes;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.components.JBList;
import org.testin.pojo.DirectoryType;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class DialogKeyListenerImpl extends KeyAdapter {
    private final JBList<DirectoryType> list;
    private final JBPopup popup;
    private final Runnable onSubmit;

    public DialogKeyListenerImpl(final JBList<DirectoryType> list, final JBPopup popup, final Runnable onSubmit) {
        this.list = list;
        this.popup = popup;
        this.onSubmit = onSubmit;
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        int currentIdx = list.getSelectedIndex();
        int currentListSize = list.getModel().getSize();

        if (e.getKeyCode() == KeyEvent.VK_DOWN) {

            if (currentListSize > 0) {
                int newIdx = Math.min(currentListSize - 1, currentIdx + 1);
                list.setSelectedIndex(newIdx);
                list.ensureIndexIsVisible(newIdx);
            }

            e.consume();

        } else if (e.getKeyCode() == KeyEvent.VK_UP) {

            if (currentListSize > 0) {
                int newIdx = Math.max(0, currentIdx - 1);
                list.setSelectedIndex(newIdx);
                list.ensureIndexIsVisible(newIdx);
            }

            e.consume();

        } else if (e.getKeyCode() == KeyEvent.VK_ENTER)
            onSubmit.run();

        else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
            popup.cancel();

    }
}