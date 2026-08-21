package org.testin.ui.framework;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable confirmation on the framework: a message (optionally with From/To
 * transfer rows), Enter runs the named action, Escape cancels. The call site
 * is the declaration — title, message, action name.
 */
public final class ConfirmDialog extends AbstractFrameworkDialog<DialogMessage> {

    private final @NotNull Runnable onConfirm;

    public ConfirmDialog(final @NotNull Project p, final @NotNull String dialogTitle, final @NotNull String message,
                         final @NotNull String from, final @NotNull String to,
                         final @NotNull String confirmName, final @NotNull Runnable onConfirm) {
        this(p, dialogTitle, message, from, to, confirmName, onConfirm, List.of());
    }

    /**
     * The same, with answers between doing it and walking away - "review the
     * changes first" beside "switch anyway" and "cancel".
     * <p>
     * A list rather than a second dialog class: what a confirmation offers is
     * the question's shape, not a different kind of dialog, and every one of
     * them is already declared as named keys on the status bar.
     *
     * @param alternatives extra answers, each closing the dialog before it runs.
     *                     Empty for the ordinary yes-or-no
     */
    public ConfirmDialog(final @NotNull Project p, final @NotNull String dialogTitle, final @NotNull String message,
                         final @NotNull String from, final @NotNull String to,
                         final @NotNull String confirmName, final @NotNull Runnable onConfirm,
                         final @NotNull List<Alternative> alternatives) {
        super(p);
        this.onConfirm = onConfirm;

        title = dialogTitle;

        components = List.of(ComponentDialogBase.message(message, from, to));

        final List<StatusBarShortcut> keys = new ArrayList<>();
        keys.add(StatusBarShortcut.build(Shortcuts.Enter, confirmName, this::submit));

        for (final Alternative alternative : alternatives) {
            keys.add(StatusBarShortcut.build(alternative.key(), alternative.name(), () -> {
                // Closed first: the answer opens something of its own, and a
                // confirmation left standing behind it is a question already
                // answered.
                closeCancel();
                alternative.action().run();
            }));
        }

        keys.add(StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel));
        shortcuts = List.copyOf(keys);
    }

    /**
     * One extra answer: the key it is on, what it is called on the status bar,
     * and what it does.
     */
    public record Alternative(@NotNull Shortcuts key, @NotNull String name, @NotNull Runnable action) {
    }

    @Override
    protected void submit() {
        onConfirm.run();
        closeOk();
    }
}
