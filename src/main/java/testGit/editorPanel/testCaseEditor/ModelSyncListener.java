package testGit.editorPanel.testCaseEditor;

import com.intellij.ui.CollectionListModel;
import lombok.Setter;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.List;

public class ModelSyncListener<T> implements ListDataListener {
    private final List<T> masterList;
    private final CollectionListModel<T> model;
    private boolean active = true;
    @Setter
    private Runnable onUpdate;

    public ModelSyncListener(List<T> masterList, CollectionListModel<T> model) {
        this.masterList = masterList;
        this.model = model;
    }

    public void pause() {
        this.active = false;
    }

    public void resume() {
        this.active = true;
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
        if (!active) return;
        for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
            T item = model.getElementAt(i);
            if (!masterList.contains(item)) {
                masterList.add(item);
            }
        }

        if (onUpdate != null) {
            SwingUtilities.invokeLater(onUpdate);
        }
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
    }
}