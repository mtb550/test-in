package org.testin.view.details.components;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextArea;
import org.testin.util.Shortcuts;

import java.awt.*;
import java.util.List;

/**
 * The whole of what a run recorded about a failure, for reading rather than
 * editing: the message and every line of the stacktrace behind it.
 * <p>
 * The details panel shows the first few lines, because a stacktrace is fifty
 * lines of framework plumbing around the two that name the tester's own code,
 * and a panel that gave it all its space would be a panel about stacktraces.
 * This is where the rest of it lives.
 * <p>
 * Nothing is saved. The area is the framework's ordinary one, which means the
 * text can be selected and copied into a bug report - the reason a tester opens
 * this at all - and an accidental edit closes with the dialog and changes
 * nothing. {@code FailedResultDialog} is where a failure is written; this only
 * reads.
 */
public final class ErrorDetailsDialog extends AbstractFrameworkDialog<TextArea> {

    private static final int VISIBLE_ROWS = 22;
    private static final int WIDTH = 900;
    private static final int HEIGHT = 600;

    public ErrorDetailsDialog(final @NotNull Project p, final @NotNull String caseDescription, final @NotNull String message, final @NotNull String stacktrace) {
        super(p);

        title = "Error";

        // Sized rather than left to its content: a stacktrace sizes to its
        // longest line, which is a fully qualified name with a path in it, and
        // a dialog that wide is unreadable. Setting it also makes the popup
        // resizable, so a tester who wants the long lines can have them.
        preferredSize = new Dimension(WIDTH, HEIGHT);

        components = List.of(
                ComponentDialogBase.details()
                        .row("Test Case", caseDescription)
                        .build(),
                ComponentDialogBase.textArea()
                        .value(fullText(message, stacktrace))
                        .rows(VISIBLE_ROWS)
                        .build());

        shortcuts = List.of(StatusBarShortcut.build(Shortcuts.Escape, "Close", this::closeCancel));
    }

    /**
     * The message above the frames, with a blank line between them, which is
     * how the IDE's own test console lays it out. A failure with no stacktrace
     * is just the message, and one with no message is just the frames - neither
     * gets a leading or trailing blank line for the part that is not there.
     */
    private static @NotNull String fullText(final @NotNull String message, final @NotNull String stacktrace) {
        if (message.isBlank()) return stacktrace;
        if (stacktrace.isBlank()) return message;

        return message + "\n\n" + stacktrace;
    }

    @Override
    protected void submit() {
        closeOk();
    }
}
