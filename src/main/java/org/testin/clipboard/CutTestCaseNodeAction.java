package org.testin.clipboard;

import org.testin.notifications.Done;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.editor.TestinEditor;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Mapper;

import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Optional;

/**
 * Declared in {@code plugin.xml} (#119) with CTRL+SHIFT+X, which the grid does
 * not claim - the plain CTRL+X is the grid's own cut, and this one acts on the
 * node.
 */
public class CutTestCaseNodeAction extends DumbAwareAction {

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        final @NotNull Optional<TestinEditor> found = TestinData.editor(e);
        if (p == null || found.isEmpty()) return;

        final @NotNull TestinEditor editor = found.orElseThrow();
        final @NotNull List<TestCaseDto> selectedTestCases = TestinData.selectedCases(e);

        if (!selectedTestCases.isEmpty()) {
            try {
                Services.getInstance(p, CutState.class).cut(editor, selectedTestCases);

                String json = Services.getInstance(p, Mapper.class).writeValueAsString(selectedTestCases);
                CopyPasteManager.getInstance().setContents(new StringSelection(json));

                // The cards draw a cut case faded, so the editor redraws from
                // what it is holding rather than this reaching for its list.
                editor.refreshView();

                Services.getInstance(p, Notifier.class).softShowCounted(p, Done.CUT, selectedTestCases.size());

            } catch (final Exception ex) {
                Logger.error("Exception: " + ex.getMessage());
            }
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Rule-EDITOR-PANEL-214. Gray with the reason on a node that cannot lose
        // a test case, rather than absent from that editor's menu (#248).
        if (TestinData.editor(e).filter(editor -> !editor.getParent().isTestCaseContainer()).isPresent()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setDescription(Bundle.message("cut.case.disabled.description"));
            return;
        }

        e.getPresentation().setEnabled(!TestinData.selectedCases(e).isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
