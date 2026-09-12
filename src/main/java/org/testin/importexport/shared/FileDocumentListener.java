package org.testin.importexport.shared;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.FileTypes;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@AllArgsConstructor
public class FileDocumentListener implements DocumentListener {

    /**
     * UC-SHARE-005, Rule-SHARE-107.
     * <p>
     * How long after the last keystroke the file is read.
     * <p>
     * The same three tenths of a second the editor's search waits
     * (Rule-EDITOR-PANEL-090), and for the same reason: a tester typing a path
     * means the path they finish, not each of its twenty prefixes. Every one of
     * those used to parse the whole workbook (#266).
     */
    private static final long QUIET_MILLIS = 300;

    private final @NotNull TextFieldWithBrowseButton fileField;
    private final @NotNull Project p;

    /**
     * What the form says while a file is being read, and empty when it has
     * stopped. The dialog is where the tester is looking, so it is where the
     * work is reported - a progress bar in the IDE's status bar would be behind
     * the window they are watching.
     */
    private final @NotNull Consumer<String> onStatus;

    private final @NotNull BiConsumer<FileTypes, Map<String, List<TestCaseDto>>> onDataLoaded;
    private final @NotNull BiFunction<File, FileTypes, Map<String, List<TestCaseDto>>> importLoader;

    /**
     * The path the newest keystroke left in the box. A read that starts and
     * finds this changed underneath it was booked by a keystroke that has been
     * typed over, so it is not the read anybody is waiting for.
     */
    private final @NotNull AtomicReference<String> awaiting = new AtomicReference<>("");


    @Override
    public void insertUpdate(final @NotNull DocumentEvent e) {
        triggerLoadIfValid();
    }

    @Override
    public void removeUpdate(final @NotNull DocumentEvent e) {
        triggerLoadIfValid();
    }

    @Override
    public void changedUpdate(final @NotNull DocumentEvent e) {
        triggerLoadIfValid();
    }

    /**
     * UC-SHARE-005, Rule-SHARE-028, Rule-SHARE-107.
     * <p>
     * Books a read for three tenths of a second from now, and lets the keystroke
     * after this one replace it.
     * <p>
     * On the EDT - this is a document listener - so reading the box here and
     * comparing off the thread later is the whole of the coordination.
     */
    private void triggerLoadIfValid() {
        final @NotNull String typed = fileField.getText().trim();
        awaiting.set(typed);

        if (typed.isEmpty()) {
            onStatus.accept("");
            return;
        }

        AppExecutorUtil.getAppScheduledExecutorService()
                .schedule(() -> readIfStillWanted(typed), QUIET_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * The read, unless the box has moved on since it was booked.
     */
    private void readIfStillWanted(final @NotNull String typed) {
        if (!isStillWanted(typed)) return;

        final @NotNull File importFile = new File(typed);

        // A path half typed is not a file yet, and says nothing: the tester is
        // still typing it, which is not a mistake to report.
        if (!importFile.exists() || !importFile.isFile()) return;

        loadFile(importFile, typed);
    }

    /**
     * Whether the box still holds the path this read was booked for.
     * <p>
     * Asked before the read starts and again when it comes back, because
     * parsing a workbook is slow and typing is not. It used to be asked only
     * before: a large file A finishing after the tester had picked B showed A's
     * sheets under B's name, and Import then wrote A's cases (#66, finding 87).
     */
    private boolean isStillWanted(final @NotNull String typed) {
        return typed.equals(awaiting.get());
    }

    private void loadFile(final @NotNull File importFile, final @NotNull String typed) {
        // Said out loud. A file no format can read fell through here in silence,
        // so a tester who picked a .pdf watched the dialog do nothing at all and
        // had no way to tell that from a file Testin was still reading (#267).
        // The formats are named rather than counted: the tester's next move is
        // to go and find one.
        FileTypes.importerFor(importFile.getName().toLowerCase())
                .ifPresentOrElse(format -> loadFile(importFile, format, typed),
                        () -> Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("import.cannot.title"),
                                Bundle.message("import.cannot.message", importFile.getName(), FileTypes.importableExtensions())));
    }

    /**
     * UC-SHARE-005, Rule-SHARE-107.
     * <p>
     * Reads the file, saying so while it does.
     * <p>
     * Parsing a workbook is heavy I/O and stays off the EDT. What is new is that
     * the dialog says it is happening: a large workbook used to leave the window
     * looking frozen, with nothing to tell a tester it was working from a tester
     * whose file could not be read at all (#266).
     * <p>
     * The line is cleared on every way out - read, empty, or failed - so it
     * never outlives the work it describes.
     */
    private void loadFile(final @NotNull File importFile, final @NotNull FileTypes format, final @NotNull String typed) {
        onStatus.accept(Bundle.message("import.reading", importFile.getName()));

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final @NotNull Map<String, List<TestCaseDto>> parsedData = importLoader.apply(importFile, format);

                ApplicationManager.getApplication().invokeLater(() -> {
                    onStatus.accept("");

                    // The box may have moved on while this was parsing, and what
                    // came back is then the wrong file's contents.
                    if (!isStillWanted(typed)) return;

                    if (parsedData.isEmpty()) {
                        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("import.no.data.title"), Bundle.message("import.no.data.message"));
                        return;
                    }
                    onDataLoaded.accept(format, parsedData);
                });

            } catch (final Exception ex) {
                Logger.error("Import parse failed: " + ex.getMessage());
                ApplicationManager.getApplication().invokeLater(() -> {
                    onStatus.accept("");

                    // Nor is a file the tester has already typed over worth a
                    // complaint about.
                    if (isStillWanted(typed)) {
                        Services.getInstance(p, Notifier.class).error(p, Bundle.message("import.parse.error.title"), ex.getMessage());
                    }
                });
            }
        });
    }
}
