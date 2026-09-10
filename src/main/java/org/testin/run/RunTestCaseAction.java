package org.testin.run;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.editor.CardHoverAction;
import org.testin.model.dto.TestCaseDto;

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
 * <p>
 * Declared in {@code plugin.xml} (#119). Its name and icon are still written
 * here rather than taken from the XML, because both change with what the
 * selection is doing - the XML's text is the name Find Action lists it under,
 * and update() says what this press would do right now.
 */
public class RunTestCaseAction extends DumbAwareAction {

    // UC-EDITOR-PANEL-035, Rule-EDITOR-PANEL-150
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        final @NotNull List<TestCaseDto> selected = TestinData.selectedCases(e);
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
            TestinData.editor(e).ifPresent(ui -> selected.forEach(tc -> ui.launching(tc.getId())));
        }

        gesture.execute(p, selected);
    }

    // UC-CODEGEN-009, Rule-CODEGEN-036
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        final @NotNull List<TestCaseDto> selected = TestinData.selectedCases(e);
        final @NotNull CardHoverAction offered = CardHoverAction.runSlot(p, selected);

        e.getPresentation().setText(offered.getTooltip());
        e.getPresentation().setIcon(offered.getIcon());

        // Grayed with the reason when a plugin it needs is missing, rather than
        // left out of the menu (#248).
        if (!offered.enableOrExplain(e.getPresentation())) return;

        e.getPresentation().setEnabled(!selected.isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
