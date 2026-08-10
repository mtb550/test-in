package org.testin.nodeCreator.dialogs;

import com.intellij.ui.components.JBList;
import org.testin.enums.DirectoryType;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Map;

public class DialogKeyListenerImpl extends KeyAdapter {
    private final JBList<DirectoryType> list;
    private final Map<Integer, KeyBinding> keyBindings;

    public DialogKeyListenerImpl(final JBList<DirectoryType> list) {
        this.list = list;
        this.keyBindings = Map.of(
                KeyEvent.VK_DOWN, new KeyBinding(() -> moveBy(1), true),
                KeyEvent.VK_UP, new KeyBinding(() -> moveBy(-1), true)
        );
    }

    private void moveBy(final int delta) {
        int currentIdx = list.getSelectedIndex();
        int currentListSize = list.getModel().getSize();
        if (currentListSize > 0) {
            int newIdx = delta > 0
                    ? Math.min(currentListSize - 1, currentIdx + delta)
                    : Math.max(0, currentIdx + delta);
            list.setSelectedIndex(newIdx);
            list.ensureIndexIsVisible(newIdx);
        }
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        final KeyBinding binding = keyBindings.get(e.getKeyCode());
        if (binding != null) {
            binding.action().run();
            if (binding.consume()) e.consume();
        }
    }
}
