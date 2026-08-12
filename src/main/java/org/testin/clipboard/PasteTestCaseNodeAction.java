package org.testin.clipboard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.testEditor.TestEditor;
import org.testin.editorPanel.testEditor.TestEditorContextMenu;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Mapper;
import org.testin.util.Tools;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PasteTestCaseNodeAction extends DumbAwareAction {

    private static final KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
    private final @NotNull Project p;
    private final @NotNull IEditor editor;

    public PasteTestCaseNodeAction(final @NotNull Project p, final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super("Paste Node", "Paste selected test cases from clipboard", AllIcons.Actions.MenuPaste);
        this.p = p;
        this.editor = editor;
        this.registerCustomShortcutSet(Tools.customShortcut(SHORTCUT), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        List<TestCaseDto> pastedCases = getFromClipboard(p);
        if (pastedCases.isEmpty()) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            TestEditor destUI = (editor instanceof TestEditor) ? (TestEditor) editor : null;
            if (destUI == null) return;

            boolean isCut = TestEditorContextMenu.isGlobalCutAction();
            IEditor sourceUI = TestEditorContextMenu.getGlobalSourceEditorUI();

            if (isCut && sourceUI != null) {

                List<TestCaseDto> cutItems = sourceUI.getAllTestCases().stream()
                        .filter(tc -> TestEditorContextMenu.getGlobalPendingCutIds().contains(tc.getId()))
                        .toList();

                ApplicationManager.getApplication().runWriteAction(() -> {
                    final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                    for (final TestCaseDto tc : cutItems) {
                        indexer.removeTestCase(sourceUI.getParent().getPath(), tc.getId());
                    }
                });

                sourceUI.getAllTestCases().removeAll(cutItems);
                if (sourceUI != destUI && sourceUI instanceof TestEditor sourceEditor) {
                    sourceEditor.resortAndPersistSequence();
                }
            }

            for (TestCaseDto tc : pastedCases) {
                if (tc == null) continue;

                TestCaseDto clonedTc = cloneForPasting(p, tc, isCut);

                if (destUI.getParent() != null) {
                    clonedTc.setParent(destUI.getParent());
                }
                destUI.getAllTestCases().add(clonedTc);
            }

            destUI.resortAndPersistSequence();

            if (isCut) {
                TestEditorContextMenu.clearCutState();
            }
        });
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {

        boolean enabled = false;
        Transferable contents = CopyPasteManager.getInstance().getContents();
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String json = (String) contents.getTransferData(DataFlavor.stringFlavor);
                if (json.trim().startsWith("[")) {
                    List<TestCaseDto> parsedList = Services.getInstance(p, Mapper.class).readValue(json, new TypeReference<>() {
                    });
                    enabled = !parsedList.isEmpty();
                }
            } catch (final Exception ex) {
                Logger.warn("[WARNING] Failed to parse clipboard JSON: " + ex.getMessage());
            }
        }
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - the clipboard is BGT-safe and update() reads no Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }

    private List<TestCaseDto> getFromClipboard(final @NotNull Project p) {
        Transferable contents = CopyPasteManager.getInstance().getContents();
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String json = (String) contents.getTransferData(DataFlavor.stringFlavor);

                return Services.getInstance(p, Mapper.class).readValue(json, new TypeReference<>() {
                });

            } catch (final Exception ex) {
                Logger.warn("[WARNING] Failed to parse clipboard JSON: " + ex.getMessage());
            }
        }
        return Collections.emptyList();
    }

    private TestCaseDto cloneForPasting(final @NotNull Project p, final TestCaseDto original, final boolean isCut) {
        final ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        final TestCaseDto clonedTc = Services.getInstance(p, Mapper.class).convertValue(original, TestCaseDto.class);

        if (isCut) {
            clonedTc.setUpdatedAt(now);
        } else {
            clonedTc.setId(UUID.randomUUID())
                    .setDescription(original.getDescription() + " (Copy)")
                    .setCreatedAt(now)
                    .setUpdatedAt(now);
        }

        return clonedTc;
    }
}