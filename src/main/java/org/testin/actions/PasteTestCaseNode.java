package org.testin.actions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditorUI;
import org.testin.editorPanel.testEditor.TestEditorCM;
import org.testin.editorPanel.testEditor.TestEditorUI;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.Mapper;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PasteTestCaseNode extends DumbAwareAction {
    private final IEditorUI editorUI;

    public PasteTestCaseNode(final IEditorUI editorUI, final JComponent component) {
        super("Paste Node", "Paste selected test cases from clipboard", AllIcons.Actions.MenuPaste);
        this.editorUI = editorUI;
        this.registerCustomShortcutSet(KeyboardSet.PasteTestCaseNode.getCustomShortcut(), component);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;
        final Project project = e.getProject();
        List<TestCaseDto> pastedCases = getFromClipboard(project);
        if (pastedCases.isEmpty()) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            TestEditorUI destUI = (editorUI instanceof TestEditorUI) ? (TestEditorUI) editorUI : null;
            if (destUI == null) return;

            boolean isCut = TestEditorCM.isGlobalCutAction();
            IEditorUI sourceUI = TestEditorCM.getGlobalSourceEditorUI();

            if (isCut && sourceUI != null) {

                List<TestCaseDto> cutItems = sourceUI.getAllTestCases().stream()
                        .filter(tc -> TestEditorCM.getGlobalPendingCutIds().contains(tc.getId()))
                        .collect(Collectors.toList());

                ApplicationManager.getApplication().runWriteAction(() ->
                        RemoveTestCase.deletePhysicalFiles(cutItems, sourceUI.getVf().getTestSet().getPath(), this));

                sourceUI.getAllTestCases().removeAll(cutItems);
                if (sourceUI != destUI && sourceUI instanceof TestEditorUI) {
                    ((TestEditorUI) sourceUI).sortAndIdentifyUnsorted();
                    sourceUI.updateSequenceAndSaveAll();
                }
            }

            for (TestCaseDto tc : pastedCases) {
                if (tc == null) continue;

                TestCaseDto clonedTc = cloneForPasting(project, tc, isCut);
                if (clonedTc == null) continue;

                if (destUI.getVf() != null && destUI.getVf().getTestSet() != null) {
                    clonedTc.setPath(destUI.getVf().getTestSet().getPath2());
                }
                destUI.getAllTestCases().add(clonedTc);
            }

            destUI.sortAndIdentifyUnsorted();
            destUI.updateSequenceAndSaveAll();

            if (isCut) {
                TestEditorCM.clearCutState();
            }
        });
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        boolean enabled = false;
        Transferable contents = CopyPasteManager.getInstance().getContents();
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String json = (String) contents.getTransferData(DataFlavor.stringFlavor);
                if (json.trim().startsWith("[")) {
                    List<TestCaseDto> parsedList = Services.getInstance(e.getProject(), Mapper.class).readValue(json, new TypeReference<>() {
                    });
                    enabled = parsedList != null && !parsedList.isEmpty();
                }
            } catch (Exception ex) {
                Log.warn("[WARNING] Failed to parse clipboard JSON: " + ex.getMessage());
            }
        }
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    private List<TestCaseDto> getFromClipboard(final @NotNull Project project) {
        Transferable contents = CopyPasteManager.getInstance().getContents();
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String json = (String) contents.getTransferData(DataFlavor.stringFlavor);

                List<TestCaseDto> parsedList = Services.getInstance(project, Mapper.class).readValue(json, new TypeReference<>() {
                });
                return parsedList != null ? parsedList : Collections.emptyList();

            } catch (Exception ex) {
                Log.warn("[WARNING] Failed to parse clipboard JSON: " + ex.getMessage());
            }
        }
        return Collections.emptyList();
    }

    private TestCaseDto cloneForPasting(final @NotNull Project project, final TestCaseDto original, final boolean isCut) {
        final ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        final TestCaseDto clonedTc = Services.getInstance(project, Mapper.class).convertValue(original, TestCaseDto.class);

        if (clonedTc != null) {
            if (isCut) {
                clonedTc.setUpdatedAt(now);
            } else {
                clonedTc.setId(UUID.randomUUID())
                        .setDescription(original.getDescription() + " (Copy)")
                        .setCreatedAt(now)
                        .setUpdatedAt(now);
            }
        }

        return clonedTc;
    }
}