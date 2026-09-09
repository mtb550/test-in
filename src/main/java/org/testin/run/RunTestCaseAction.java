package org.testin.run;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.editor.CardHoverAction;
import org.testin.editor.TestinEditor;
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

    /**
     * The editor this entry belongs to, told before a run starts so an execution
     * report can be traced back to the run the tester started it from.
     */
    private final @NotNull TestinEditor ui;

    public RunTestCaseAction(final @NotNull Project p, final @NotNull TestinEditor ui, final @NotNull JBList<TestCaseDto> list) {
        super(p, CardHoverAction.RUN_TEST_CASE.getTooltip(),
                "Run the selected test cases, or stop a run that is going",
                CardHoverAction.RUN_TEST_CASE.getIcon());
        this.ui = ui;
        this.list = list;
        this.registerCustomShortcutSet(Shortcuts.RunTestCase.getCustomShortcut(), list);
    }

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-150
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @NotNull List<TestCaseDto> selected = list.getSelectedValuesList();
        final @NotNull CardHoverAction gesture = CardHoverAction.runSlot(p, selected);

        // Which editor the tester asked from, the same thing a click on the
        // card's run icon says. Without it a run started from the menu is a
        // report nobody claims, and the run records nothing.
        //
        // Only on the gesture that starts something, and after asking which it
        // is. Claiming stamps when execution began and moves a Created run to In
        // Progress with a message, so claiming everything first made F5 - the
        // stop gesture - start the run it was stopping (#221).
        if (gesture == CardHoverAction.RUN_TEST_CASE) {
            selected.forEach(tc -> ui.launching(tc.getId()));
        }

        gesture.execute(p, selected);
    }

    // UC-CODEGEN-009, Rule-CODEGEN-036
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull List<TestCaseDto> selected = list.getSelectedValuesList();
        final @NotNull CardHoverAction offered = CardHoverAction.runSlot(p, selected);

        e.getPresentation().setText(offered.getTooltip());
        e.getPresentation().setIcon(offered.getIcon());

        // Grayed with the reason when a plugin it needs is missing, rather than
        // left out of the menu (#248).
        if (!offered.enableOrExplain(e.getPresentation())) return;

        e.getPresentation().setEnabled(!list.isEmpty() && !selected.isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
