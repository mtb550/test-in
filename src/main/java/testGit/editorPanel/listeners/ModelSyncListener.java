package testGit.editorPanel.listeners;

import com.intellij.ui.CollectionListModel;
import lombok.Setter;
import testGit.editorPanel.testCaseEditor.TestEditorUI;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;

public class ModelSyncListener implements ListDataListener {
    private final TestEditorUI ui;
    private final CollectionListModel<TestCaseDto> model;
    private boolean active = true;
    @Setter
    private UpdateCallback onUpdateCallback;

    public ModelSyncListener(final TestEditorUI ui, final CollectionListModel<TestCaseDto> model) {
        this.ui = ui;
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

        int globalStart = (ui.getCurrentPage() - 1) * ui.getPageSize() + e.getIndex0();
        TestCaseDto newlyAdded = null;

        for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
            TestCaseDto item = model.getElementAt(i);
            if (!ui.getAllTestCaseDtos().contains(item)) {
                ui.getAllTestCaseDtos().add(globalStart++, item);
                newlyAdded = item;
            }
        }

        ui.updateSequenceAndSaveAll();

        if (onUpdateCallback != null) {
            SwingUtilities.invokeLater(() -> onUpdateCallback.onUpdate());
        }

        if (newlyAdded != null) {
            final TestCaseDto target = newlyAdded;
            SwingUtilities.invokeLater(() -> ui.selectTestCase(target));
        }
    }

    @Override
    public void intervalRemoved(final ListDataEvent e) {
        if (!active) return;

        int globalStart = (ui.getCurrentPage() - 1) * ui.getPageSize();
        List<TestCaseDto> allItems = ui.getAllTestCaseDtos();

        if (globalStart >= allItems.size()) return;

        int pageEnd = Math.min(globalStart + ui.getPageSize(), allItems.size());

        List<TestCaseDto> pageInMaster;
        synchronized (allItems) {
            pageInMaster = new ArrayList<>(allItems.subList(globalStart, pageEnd));
        }

        List<TestCaseDto> pageInModel = model.getItems();

        boolean changed = false;
        for (TestCaseDto tc : pageInMaster) {
            if (!pageInModel.contains(tc)) {
                allItems.remove(tc);
                changed = true;
            }
        }

        if (changed) {
            ui.updateSequenceAndSaveAll();
        }

        if (onUpdateCallback != null) {
            SwingUtilities.invokeLater(() -> onUpdateCallback.onUpdate());
        }
    }

    @Override
    public void contentsChanged(final ListDataEvent e) {
    }

    public interface UpdateCallback {
        void onUpdate();
    }
}