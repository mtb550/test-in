package org.testin.ui.framework;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.util.Shortcuts;

import java.util.List;

/**
 * Reusable confirmation on the framework: a message (optionally with From/To
 * transfer rows), Enter runs the named action, Escape cancels. The call site
 * is the declaration — title, message, action name.
 */
public final class ConfirmDialog extends AbstractFrameworkDialog<DialogMessage> {

    private final @NotNull Runnable onConfirm;

    public ConfirmDialog(final @NotNull Project p, final @NotNull String dialogTitle, final @NotNull String message,
                         final @Nullable String from, final @Nullable String to,
                         final @NotNull String confirmName, final @NotNull Runnable onConfirm) {
        super(p);
        this.onConfirm = onConfirm;

        title = dialogTitle;

        components = List.of(ComponentDialogBase.message(message, from, to));

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, confirmName, this::submit),
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel));
    }

    @Override
    protected void submit() {
        onConfirm.run();
        closeOk();
    }
}
