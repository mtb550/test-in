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
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

import org.testin.services.OptionalPlugin;
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

    /**
     * UC-CODEGEN-005, Rule-CODEGEN-025, Rule-CODEGEN-071.
     * <p>
     * Nothing, and nothing can reach it: {@link #update} ends disabled on every
     * path, so neither the menu entry nor the shortcut invokes this. The entry
     * says why it is gray, which is the whole of what this action does today.
     * <p>
     * It held a refusal balloon saying "not built yet", which no tester could
     * ever have read - a sentence to keep right forever for no reader. The work
     * goes here when #243 builds it and enables the entry.
     */
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
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
