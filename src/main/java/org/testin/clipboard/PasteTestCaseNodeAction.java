package org.testin.clipboard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.editor.TestinEditor;
import org.testin.editor.test.TestEditor;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Mapper;
import org.testin.util.Shortcuts;

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

public class PasteTestCaseNodeAction extends AbstractProjectAction {

    private static final KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
    private final @NotNull TestinEditor editor;

    public PasteTestCaseNodeAction(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Paste Node", "Paste selected test cases from clipboard", AllIcons.Actions.MenuPaste);
        this.editor = editor;
        this.registerCustomShortcutSet(Shortcuts.customShortcut(SHORTCUT), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        List<TestCaseDto> pastedCases = getFromClipboard(p);
        if (pastedCases.isEmpty()) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            TestEditor destUI = (editor instanceof TestEditor) ? (TestEditor) editor : null;
            if (destUI == null) return;

            final CutState cutState = Services.getInstance(p, CutState.class);
            final boolean isCut = cutState.isCutting();

            cutState.source().ifPresent(sourceUI -> {
                final List<TestCaseDto> cutItems = sourceUI.getAllTestCases().stream()
                        .filter(tc -> cutState.isPending(tc.getId()))
                        .toList();

                ApplicationManager.getApplication().runWriteAction(() -> {
                    final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                    for (final TestCaseDto tc : cutItems) {
                        indexer.removeTestCase(sourceUI.getParent().getPath(), tc.getId());
                    }
                });

                sourceUI.getAllTestCases().removeAll(cutItems);
                if (sourceUI != destUI && sourceUI instanceof TestEditor sourceEditor) {
                    sourceEditor.reorderAndPersist();
                }
            });

            int pasted = 0;

            for (TestCaseDto tc : pastedCases) {
                if (tc == null) continue;

                TestCaseDto clonedTc = cloneForPasting(p, tc, isCut);

                clonedTc.setParent(destUI.getParent());
                destUI.getAllTestCases().add(clonedTc);
                pasted++;
            }

            destUI.reorderAndPersist();

            if (isCut) cutState.clear();

            // Inside the invokeLater and after the sequence is persisted: the
            // action itself returns long before the cases exist (#62).
            if (pasted > 0) Services.getInstance(p, Notifier.class).softShowCounted(p, "Pasted", pasted);
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

    private @NotNull TestCaseDto cloneForPasting(final @NotNull Project p, final @NotNull TestCaseDto original, final boolean isCut) {
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
