package org.testin.automate;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;

import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.util.Bundle;


/**
 * UC-CODEGEN-005.
 * <p>
 * Declared in {@code plugin.xml} (#119), with Ctrl+F12 as its default. Find
 * Action is where a tester goes looking for "automate", and this entry saying
 * "not built yet" there is a better answer than nothing being found.
 */
public class AutomateTestCaseAction extends DumbAwareAction {

    /**
     * What the entry is called while it does nothing: the name, and the reason
     * in the same breath. On the entry rather than in a message, so a tester
     * reads it before pressing rather than after.
     */
    private static final @NotNull String NOT_BUILT = Bundle.message("automate.not.built.text");

    // UC-CODEGEN-005, Rule-CODEGEN-025, Rule-CODEGEN-071
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        // update() disables the action on an empty selection, but the shortcut
        // and the selection can race, so Swing can still answer with nothing.
        //
        // Generating the code is #243, which also decides whether this entry
        // stays on the menu until it is built.
        TestinData.selectedCases(e).stream().findFirst().ifPresent(tc -> Logger.info(tc.getDescription()));

        // Says so until then. This is the one action in the menu that changes
        // nothing, and silence here reads as a bug rather than as unbuilt: after
        // #62 every other action confirms itself, so the odd one out is the one
        // that answers with nothing at all (#66, F4).
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("automate.not.built.title"),
                Bundle.message("automate.not.built.message"));
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
        e.getPresentation().setDescription(Bundle.message("automate.not.built.description"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
