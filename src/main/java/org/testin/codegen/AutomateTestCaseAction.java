/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.codegen;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Computable;
import org.testin.logger.Logger;
import org.testin.navigate.CodeNavigation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.project.Project;

import org.testin.actions.TestinData;
import org.testin.editor.TestinEditor;
import org.testin.model.Automated;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.OptionalPlugin;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;

/**
 * UC-CODEGEN-005.
 * <p>
 * Writes the automation method for a test case that has none.
 * <p>
 * Declared in {@code plugin.xml} (#119) with F12. The entry was a placeholder
 * for a long time - gray, and named <i>Automate Test Case (not built yet)</i> -
 * because the only thing that wrote a method was saving a test case with a
 * description, and there was nothing to put behind an entry that acted on a case
 * already stored.
 * <p>
 * <b>What made it buildable is that the generator does all of it now.</b> A test
 * case arrives without a method every time one comes in as data rather than
 * through the create dialog: a Git pull, an SFTP sync, an imported sheet, a
 * branch switch, a data root edited by hand. Nothing generated for those cases,
 * and nothing could be asked to - Run and Navigate to Code refused for every one
 * of them and only the log said why. {@code CreateTestMethod} already writes the
 * class and its package folders, skips a method that is there, adopts one the
 * tester wrote by hand and reports a name another case has taken, so this action
 * is the gesture in front of work that was already finished.
 * <p>
 * <b>It writes the declaration, not the steps.</b> Filling in what the method
 * does is a different feature and keeps its own issue (#3). What this gives the
 * tester is the method their test case should have had, in the class it belongs
 * in, ready to be filled.
 */
public class AutomateTestCaseAction extends DumbAwareAction {

    /**
     * UC-CODEGEN-005, Rule-CODEGEN-025.
     * <p>
     * Generates for the selected cases that have no method, and leaves the rest
     * alone.
     * <p>
     * Through the same operation creating a test case runs, which is the point:
     * a method written by this and a method written by the create dialog are the
     * same method, because they are the same generator. It carries the class and
     * the package folders with it for a test set nothing has generated into yet.
     */
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        final @NotNull List<TestCaseDto> toWrite =
                withoutAMethod(p, TestinData.selectedCases(e).stream().filter(AutomateTestCaseAction::canBeNamed).toList());
        if (toWrite.isEmpty()) return;

        // Taken now rather than in the callback below: an AnActionEvent is good
        // for the length of the action and no longer.
        final @NotNull Optional<TestinEditor> editor = TestinData.editor(e);

        GenType.CREATE_TEST_CASE.executeAll(p, toWrite);

        // Both of these belong behind the write, which happens in a later event.
        // A balloon shown here would say Automated before anything had been, and
        // the redraw would read the class as it was - the same mistake the paste
        // path made until it moved its message into the callback (#312, A56).
        ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final int written = writtenFor(p, toWrite);

            ApplicationManager.getApplication().invokeLater(() -> {
                // Rule-CODEGEN-025. Counting what now has a method, not what was
                // asked for. The count was the size of the request, so a project
                // with no Java test source folder read "Java Test Source Not
                // Found" and "Automated 3" side by side, and a case whose name
                // another method holds read "Automated 1" on every press, forever
                // (#66, finding 196). Nothing written, nothing claimed: the
                // generator has already said why, in the words that say what to
                // do about it.
                if (written > 0) Services.getInstance(p, Notifier.class).softShowCounted(p, Done.AUTOMATED, written);

                // The cards draw the automation mark from what the last read
                // found, and nothing invalidates that - a redraw is what reads
                // again. So the tester sees the mark change rather than being
                // told it changed.
                editor.ifPresent(TestinEditor::refreshView);
            });
        }));
    }

    /**
     * How many of these have a method now. None had one by the page's last read,
     * which is what put them in this list, so every one that has one now was
     * written by this gesture. Off the EDT and under a read action, as
     * {@link AutomationState} reads the same thing; nothing while the IDE indexes,
     * when the generator refused and said so.
     */
    private static int writtenFor(final @NotNull Project p, final @NotNull List<TestCaseDto> asked) {
        if (DumbService.isDumb(p)) return 0;

        try {
            return ApplicationManager.getApplication().runReadAction((Computable<Integer>) () -> CodeNavigation.available().methodsFor(p, asked).size());
        } catch (final Exception ex) {
            Logger.warn("Could not count the methods Automate Test Case wrote: " + ex.getMessage());
            return 0;
        }
    }

    /**
     * UC-CODEGEN-005, Rule-CODEGEN-071.
     * <p>
     * Live where there is a method to write, and gray with the reason where
     * there is not - never left off the menu.
     * <p>
     * The reason is worth saying rather than leaving the entry simply dead: a
     * tester who presses this on a case that is already automated has asked a
     * reasonable question, and <i>every one of these already has its method</i>
     * is the answer to it. Writing nothing and saying <i>Automated</i> would be
     * a success that did not happen.
     * <p>
     * {@link AutomationState} answers from what the page's last read found, so
     * this costs no PSI on the EDT. A case nobody has read yet counts as one to
     * write: the generator skips a method that is there, so the worst an unread
     * case can cost is a pass that writes nothing, and the alternative is
     * refusing a tester because Testin has not looked.
     * <p>
     * The plugin check runs first. Without the Java plugin the reason is that,
     * not this.
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Grayed with the reason without the Java plugin, rather than left out of
        // the menu (#248).
        if (!OptionalPlugin.JAVA.enableOrExplain(this, e.getPresentation())) return;

        final @Nullable Project p = e.getProject();
        if (p == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        final @NotNull List<TestCaseDto> selected = TestinData.selectedCases(e);
        if (selected.isEmpty()) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // A description is what names a method (Rule-CODEGEN-002), so a case
        // without one cannot be given code by anything. Said as itself rather
        // than folded into the message below, which would tell a tester their
        // case already has a method when it can never have one.
        final @NotNull List<TestCaseDto> nameable = selected.stream().filter(AutomateTestCaseAction::canBeNamed).toList();
        if (nameable.isEmpty()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setDescription(Bundle.message("automate.no.description.description"));
            return;
        }

        if (withoutAMethod(p, nameable).isEmpty()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setDescription(Bundle.message("automate.already.written.description"));
            return;
        }

        e.getPresentation().setEnabled(true);
    }

    /**
     * UC-CODEGEN-005, Rule-CODEGEN-025.
     * <p>
     * The cases there is a method to write for: the ones that have none.
     * <p>
     * <b>Not the ones that are un-automated.</b> {@link Automated#NONE} is what a
     * card draws for a case with an empty generated method as well as for a case
     * that can have no method at all, and to a tester reading a card that is
     * right - neither has automation. Asking it here left the entry live over a
     * case whose method was already there, so a second press wrote nothing and
     * said <i>Automated 1</i>. Caught in a sandbox pass on 17 September 2026,
     * twice in six seconds.
     * <p>
     * {@link AutomationState#hasMethod} is the question this one asks, out of the
     * same read and off the EDT.
     */
    private static @NotNull List<TestCaseDto> withoutAMethod(final @NotNull Project p, final @NotNull List<TestCaseDto> cases) {
        final @NotNull AutomationState state = Services.getInstance(p, AutomationState.class);

        return cases.stream().filter(tc -> !state.hasMethod(tc.getId())).toList();
    }

    /**
     * Whether this case's description can name a Java method, which is what
     * decides whether it can have one at all (Rule-CODEGEN-002).
     */
    private static boolean canBeNamed(final @NotNull TestCaseDto tc) {
        return !Fqcn.methodNameOf(tc).isEmpty();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
