package org.testin.run;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.editor.CardHoverAction;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Shortcuts;

import java.util.List;

/**
 * Run or stop the selected test cases, from the context menu or from F5.
 * <p>
 * Which of the two it is comes from {@link CardHoverAction#runSlot}, the same
 * answer a card's hover button uses, so the key and the button cannot mean
 * different things. They did: the button stopped the run while F5 called
 * {@code RunTestCases.run}, which skips a case that is already running - so the
 * key did nothing at all, and said nothing (#66, finding 18).
 * <p>
 * The entry names itself for the same reason. A menu reading "Run Test Case"
 * that stops the run would be the old disagreement wearing a different hat.
 */
public class RunTestCaseAction extends AbstractProjectAction {

    private final @NotNull JBList<TestCaseDto> list;

    public RunTestCaseAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list) {
        super(p, CardHoverAction.RUN_TEST_CASE.getTooltip(),
                "Run the selected test cases, or stop a run that is going",
                CardHoverAction.RUN_TEST_CASE.getIcon());
        this.list = list;
        this.registerCustomShortcutSet(Shortcuts.RunTestCase.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @NotNull List<TestCaseDto> selected = list.getSelectedValuesList();

        CardHoverAction.runSlot(selected).execute(p, selected);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull List<TestCaseDto> selected = list.getSelectedValuesList();
        final @NotNull CardHoverAction offered = CardHoverAction.runSlot(selected);

        e.getPresentation().setEnabled(!list.isEmpty() && !selected.isEmpty());
        e.getPresentation().setText(offered.getTooltip());
        e.getPresentation().setIcon(offered.getIcon());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
