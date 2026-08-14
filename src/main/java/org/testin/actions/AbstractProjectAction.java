package org.testin.actions;

import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     */
    protected AbstractProjectAction(final @NotNull Project p, final @NotNull String title) {
        super(title);
        this.p = p;
    }

    /**
     * The icon is nullable because an action shown only in a list has none.
     */
    protected AbstractProjectAction(final @NotNull Project p, final @NotNull String title,
                                    final @NotNull String description, final @Nullable Icon icon) {
        super(title, description, icon);
        this.p = p;
    }
}
