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

package org.testin.actions;

import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * An action that needs the project, which is almost all of them.
 * <p>
 * Every action starts its constructor with {@code super(...)}, because that is
 * how it gets its text, description and icon - and Lombok cannot generate a call
 * to a superclass constructor, so {@code @AllArgsConstructor} is not available
 * here (#59). The field and its assignment were therefore copied into every
 * action: two lines each, saying the same thing.
 * <p>
 * This holds constructor arguments and nothing else. No behavior belongs on it:
 * every action would inherit a method most of them do not want, and an action's
 * {@code actionPerformed} and {@code getActionUpdateThread} are its own.
 */
public abstract class AbstractProjectAction extends DumbAwareAction {

    protected final @NotNull Project p;

    /**
     * For an action the platform shows by name alone - a keyboard-only action
     * with no menu entry.
     * <p>
     * SameParameterValue reports that {@code title} is always "Update", which is
     * true and not a reason to inline it: {@code UpdateTestCaseAction} is simply
     * the only keyboard-only action so far, and hard-coding its name into the
     * base class would make the next one rename every action that uses it (#66).
     */
    protected AbstractProjectAction(final @NotNull Project p, final @NotNull String title, final @NotNull String description, final @NotNull Icon icon) {
        super(title, description, icon);
        this.p = p;
    }
}
