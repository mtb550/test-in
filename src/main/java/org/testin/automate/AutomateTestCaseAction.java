package org.testin.automate;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;

import java.util.Optional;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.util.Shortcuts;

import javax.swing.*;

public class AutomateTestCaseAction extends AbstractProjectAction {

    private final @NotNull JBList<TestCaseDto> list;

    public AutomateTestCaseAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Automate Test Case", "Generate automation code for the selected test case", AllIcons.Actions.IntentionBulb);
        this.list = list;
        this.registerCustomShortcutSet(Shortcuts.AutomateTestCase.getCustomShortcut(), list);
    }

    /**
     * What the entry is called while it does nothing: the name, and the reason
     * in the same breath. On the entry rather than in a message, so a tester
     * reads it before pressing rather than after.
     */
    private static final @NotNull String NOT_BUILT = "Automate Test Case (not built yet)";

    // UC-CODEGEN-005, Rule-CODEGEN-025, Rule-CODEGEN-071
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        // update() disables the action on an empty selection, but the shortcut
        // and the selection can race, so Swing can still answer with nothing.
        //
        // Generating the code is #243, which also decides whether this entry
        // stays on the menu until it is built.
        Optional.ofNullable(list.getSelectedValue()).ifPresent(tc -> Logger.info(tc.getDescription()));

        // Says so until then. This is the one action in the menu that changes
        // nothing, and silence here reads as a bug rather than as unbuilt: after
        // #62 every other action confirms itself, so the odd one out is the one
        // that answers with nothing at all (#66, F4).
        Services.getInstance(p, Notifier.class).softRefuse(p, "Not built yet",
                "Generating automation code for a test case is coming in a later release.");
    }

    /**
     * UC-CODEGEN-005, Rule-CODEGEN-071.
     * <p>
     * Gray, and saying why, until there is something behind it.
     * <p>
     * It was live on every selected test case and always answered <i>Not built
     * yet</i> - the one entry named after generating code being the one that did
     * not, and nothing on the menu saying so until it was pressed (#243). A
     * control that cannot work is shown and disabled with the reason, never left
     * out: a tester who cannot see it cannot learn it is coming.
     * <p>
     * The plugin check above it stays and runs first. Without the Java plugin the
     * reason is that, not this - there is no point promising a later release to
     * an IDE that could not run it either way.
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Grayed with the reason without the Java plugin, rather than left out of
        // the menu (#248).
        if (!OptionalPlugin.JAVA.enableOrExplain(this, e.getPresentation())) return;

        e.getPresentation().setEnabled(false);
        e.getPresentation().setText(NOT_BUILT);
        e.getPresentation().setDescription("Testin writes a test case's method when the case is saved with a description. Generating one for a case that already exists is a later release.");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
