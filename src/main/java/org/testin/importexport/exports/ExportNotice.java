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

package org.testin.importexport.exports;

import org.testin.notifications.Done;
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
import org.testin.util.Bundle;

import java.awt.*;
import java.util.Locale;
import java.util.Optional;
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
public final class ExportNotice {

    /**
     * UC-SHARE-004, Rule-SHARE-022.
     * <p>
     * For a file the operating system knows how to open: a spreadsheet, a CSV,
     * a JSON document.
     */
    static void show(final @NotNull Project p, final @NotNull File file) {
        show(p, file, () -> open(p, file));
    }

    /**
     * UC-SHARE-004.
     * <p>
     * Opens a file the plugin has just written, whatever wrote it.
     * <p>
     * Public because the report generator had its own copy of this - a bare
     * {@code Desktop.getDesktop().open}, which skipped the desktop-support check
     * below, ran on the UI thread where opening blocks until Acrobat or Word has
     * started, and reported its failure only to the log. A tester on a machine
     * that cannot open files pressed Open report and nothing happened at all.
     */
    public static void open(final @NotNull Project p, final @NotNull File file) {
        Optional.ofNullable(LocalFileSystem.getInstance().findFileByPath(file.getAbsolutePath()))
                .filter(VirtualFile::exists)
                .ifPresentOrElse(found -> openWithAssociatedProgram(p, found),
                        () -> Services.getInstance(p, Notifier.class)
                                .error(p, Bundle.message("export.open.error.title"), Bundle.message("export.open.missing")));
    }

    /**
     * Hands the file to whatever application claims its extension, and says so
     * when the desktop cannot. Here rather than in a utility class: this is the
     * only thing in the plugin that asks the operating system to open anything.
     */
    private static void openWithAssociatedProgram(final @NotNull Project p, final @NotNull VirtualFile virtualFile) {
        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);

        // A web page goes to the browser, whichever button wrote it. The same
        // .html opened in whatever application claimed the extension when it was
        // a report and in the browser when it was an export - one file, two
        // behaviours, decided by which half of the plugin made it (#256).
        if (virtualFile.getName().toLowerCase(Locale.ROOT).endsWith(".html")) {
            BrowserUtil.browse(new File(virtualFile.getPath()).toURI().toString());
            return;
        }

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            notifier.error(p, Bundle.message("export.system.error.title"), Bundle.message("export.system.unsupported"));
            return;
        }

        final @NotNull File toOpen = new File(virtualFile.getPath());
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Desktop.getDesktop().open(toOpen);
            } catch (final IOException ex) {
                ApplicationManager.getApplication().invokeLater(() ->
                        notifier.error(p, Bundle.message("export.execution.error.title"), Bundle.message("export.execution.failed", ex.getMessage())));
            }
        });
    }

    /**
     * UC-SHARE-004, Rule-SHARE-022.
     * <p>
     * For a file meant to be read in a browser. {@link #open} sends every web
     * page there anyway, so this is the caller that knows before it looks.
     */
    static void showInBrowser(final @NotNull Project p, final @NotNull File file) {
        show(p, file, () -> BrowserUtil.browse(file.toURI().toString()));
    }

    private static void show(final @NotNull Project p, final @NotNull File file, final @NotNull Runnable open) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);
            notifier.infoWithActions(p, Done.EXPORTED.getOutcome(), file.getName(),
                    notifier.action(Bundle.message("export.open.file"), open), notifier.copyPath(file));
        });
    }
}
