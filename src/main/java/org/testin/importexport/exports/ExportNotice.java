package org.testin.importexport.exports;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Says an export finished, and offers to open what it produced.
 * <p>
 * One place for all four formats. It was four copies of the same eight lines,
 * three of them identical to the character.
 * <p>
 * So the wording, the action label and the decision to raise this on the EDT
 * could each drift in one format without anyone noticing in the others.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ExportNotice {

    /**
     * For a file the operating system knows how to open: a spreadsheet, a CSV,
     * a JSON document.
     */
    static void show(final @NotNull Project p, final @NotNull File file) {
        show(p, file, () -> {
            final VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(file.getAbsolutePath());
            openWithAssociatedProgram(p, virtualFile);
        });
    }

    /**
     * Hands the file to whatever application claims its extension, and says so
     * when the desktop cannot. Here rather than in a utility class: this is the
     * only thing in the plugin that asks the operating system to open anything.
     */
    private static void openWithAssociatedProgram(final @NotNull Project p, final @Nullable VirtualFile virtualFile) {
        final Notifier notifier = Services.getInstance(p, Notifier.class);

        if (virtualFile == null || !virtualFile.exists()) {
            notifier.error(p, "Open Error", "The file does not exist.");
            return;
        }

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            notifier.error(p, "System Error", "Opening a file is not supported on this system.");
            return;
        }

        final File toOpen = new File(virtualFile.getPath());
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Desktop.getDesktop().open(toOpen);
            } catch (final IOException ex) {
                ApplicationManager.getApplication().invokeLater(() ->
                        notifier.error(p, "Execution Error", "Failed to open the file: " + ex.getMessage()));
            }
        });
    }

    /**
     * For a report meant to be read in a browser rather than handed to whatever
     * application claims the extension.
     */
    static void showInBrowser(final @NotNull Project p, final @NotNull File file) {
        show(p, file, () -> BrowserUtil.browse(file.toURI().toString()));
    }

    private static void show(final @NotNull Project p, final @NotNull File file, final @NotNull Runnable open) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final Notifier notifier = Services.getInstance(p, Notifier.class);
            notifier.infoWithActions(p, "Exported", file.getName(), notifier.action("Open file", open));
        });
    }
}
