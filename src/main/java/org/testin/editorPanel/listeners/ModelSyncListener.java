package org.testin.editorPanel.listeners;

import com.intellij.ui.CollectionListModel;
import lombok.Setter;
import org.testin.editorPanel.testEditor.TestEditor;
import org.testin.pojo.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModelSyncListener implements ListDataListener {
    private final TestEditor editor;
    private final CollectionListModel<TestCaseDto> model;
    private boolean active = true;

    @Setter
    private IUpdateCallback onUpdateCallback;

    public ModelSyncListener(final TestEditor editor, final CollectionListModel<TestCaseDto> model) {
        this.editor = editor;
        this.model = model;
    }

    public void pause() {
        this.active = false;
    }

    public void resume() {
        this.active = true;
    }

    @Override
    public void intervalAdded(final ListDataEvent e) {
        if (!active) return;

        int globalStart = (editor.getCurrentPage() - 1) * editor.getPageSize() + e.getIndex0();
        TestCaseDto newlyAdded = null;

        for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
            final TestCaseDto item = model.getElementAt(i);
            if (!editor.getAllTestCases().contains(item)) {
                editor.getAllTestCases().add(globalStart++, item);
                newlyAdded = item;
            }
        }

        editor.updateSequenceAndSaveAll();

        Optional.ofNullable(onUpdateCallback).ifPresent(cb -> SwingUtilities.invokeLater(cb::onUpdate));

        if (newlyAdded != null) {
            final TestCaseDto target = newlyAdded;
            SwingUtilities.invokeLater(() -> editor.selectTestCase(target));
        }
    }

    @Override
    public void intervalRemoved(final ListDataEvent e) {
        if (!active) return;

        final int globalStart = (editor.getCurrentPage() - 1) * editor.getPageSize();
        final List<TestCaseDto> allItems = editor.getAllTestCases();

        if (globalStart >= allItems.size()) return;

        final int pageEnd = Math.min(globalStart + editor.getPageSize(), allItems.size());
        final List<TestCaseDto> pageInMaster;

        synchronized (allItems) {
            pageInMaster = new ArrayList<>(allItems.subList(globalStart, pageEnd));
        }

        final List<TestCaseDto> pageInModel = model.getItems();

        final List<TestCaseDto> toRemove = pageInMaster.stream()
                .filter(tc -> !pageInModel.contains(tc))
                .toList();

        if (!toRemove.isEmpty()) {
            allItems.removeAll(toRemove);
            editor.updateSequenceAndSaveAll();
        }

        Optional.ofNullable(onUpdateCallback).ifPresent(cb -> SwingUtilities.invokeLater(cb::onUpdate));
    }

    @Override
    public void contentsChanged(final ListDataEvent e) {
    }

    public interface IUpdateCallback {
        void onUpdate();
    }
}