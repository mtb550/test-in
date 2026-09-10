package org.testin.view;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.model.dto.TestCaseDto;

import java.util.List;

/**
 * UC-VIEW-PANEL-001.
 * <p>
 * Declared in {@code plugin.xml} (#119) with no default key, for the reason
 * {@code Testin.Open} has none: ENTER on a list is that list's gesture rather
 * than a command, and a global ENTER would fire in every editor in the IDE. The
 * editors bind it on the declared instance, so one action sits behind the menu
 * entry and the key.
 * <p>
 * The path the panel is opened at comes from the editor rather than from a
 * field. It was handed in as {@code dir.getPath2()} when the menu built this,
 * and the editor knows its own node - so there is nothing to carry.
 */
public class ViewDetailsAction extends DumbAwareAction {

    // UC-VIEW-PANEL-001, Rule-VIEW-PANEL-011, Rule-VIEW-PANEL-013
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        final @NotNull List<TestCaseDto> selected = TestinData.selectedCases(e);
        if (selected.isEmpty()) return;

        TestinData.editor(e).ifPresent(editor ->
                ViewToolWindowFactory.showPanel(p, selected, editor.getParent().getPath2(), ViewPanel::focusDetailsTab));
    }

    // UC-VIEW-PANEL-001
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!TestinData.selectedCases(e).isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
