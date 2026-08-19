package org.testin.importexport.exports;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Tools;

import java.io.File;

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
            Services.getInstance(p, Tools.class).openWithAssociatedProgram(p, virtualFile);
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
