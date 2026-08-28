package org.testin.util;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.EditorType;
import org.testin.editor.TestinEditor;
import org.testin.editor.UnifiedFileEditor;
import org.testin.editor.UnifiedVirtualFile;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.view.ViewToolWindowFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class EditorUtil {
    private final @NotNull String OPEN_EDITORS_KEY = "testin.openEditors";

    /**
     * The open file showing this node, and empty when none is.
     * <p>
     * By path, which is the node's identity - the same match {@code openNow},
     * {@code editorFor} and the rename listener already make, and the reason
     * three of the five lookups in this class carried a comment saying so while
     * two matched on the name. Two test sets both called "Login" in different
     * packages are one name and two nodes, and the name answers for whichever
     * happened to be open first.
     */
    private @NotNull Optional<VirtualFile> openFileFor(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        for (final VirtualFile open : FileEditorManager.getInstance(p).getOpenFiles()) {
            if (!(open instanceof UnifiedVirtualFile testinFile)) continue;
            if (!testinFile.getDir().getPath().equals(dir.getPath())) continue;

            return Optional.of(open);
        }

        return Optional.empty();
    }

    /**
     * Closes the editor showing this node, if one is open.
     * <p>
     * It took the node's name and closed whichever tab matched, so renaming or
     * removing one of two same-named test sets closed the other's tab - and
     * left the affected node's editor open, holding data that had just been
     * renamed or deleted, for the next save to write back.
     */
    public void close(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        openFileFor(p, dir).ifPresent(FileEditorManager.getInstance(p)::closeFile);

    }

    /**
     * Brings every open Testin editor back in line with the index that was just
     * rebuilt: the ones whose node is still there read it again, and the ones
     * whose node is gone close.
     * <p>
     * An editor holds the node it was opened on and the test cases it read from
     * it, so after a re-index both can be wrong. A branch switch replaces every
     * file under the project - a test set that is still there holds different
     * cases, and one the branch does not have is not there at all. Left alone,
     * the first shows a colleague's old data and the second shows cases that
     * exist nowhere, and saving either would write them back.
     * <p>
     * On the EDT, after indexing has finished. Asked before the cache is rebuilt
     * it would close every editor in the project.
     */
    public void refreshOpen(final @NotNull Project p) {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(p);

        for (final VirtualFile open : fed.getOpenFiles()) {
            if (!(open instanceof UnifiedVirtualFile testinFile)) continue;

            if (!isIndexed(p, testinFile)) {
                Logger.info("Closing the editor for a node that is no longer indexed: " + testinFile.getName());
                fed.closeFile(testinFile);
                continue;
            }

            for (final FileEditor tab : fed.getAllEditors(testinFile)) {
                if (!(tab instanceof UnifiedFileEditor unified)) continue;

                final @NotNull TestinEditor editor = unified.getEditor();
                // A running run or an open grid cell is live state the tester is
                // in the middle of, and a reload throws it away - the timer stops
                // with its seconds unstamped, the half-typed cell is gone. The
                // change on disk waits for them to finish; their own Refresh
                // button, pressed on purpose, still reloads.
                if (editor.isBusy()) {
                    Logger.info("Leaving a busy editor as it is rather than reloading under the tester: "
                            + testinFile.getName());
                    continue;
                }
                editor.reload();
            }
        }
    }

    /**
     * Whether the index still holds this editor's node, asked of the index that
     * owns its kind: a run editor's node of the test runs, a test set editor's
     * of the test sets. The kind is the node's own class, which is what decided
     * the editor type when it was opened.
     */
    private boolean isIndexed(final @NotNull Project p, final @NotNull UnifiedVirtualFile file) {
        // Asked as a question rather than by fetching and comparing to null: the
        // lookups promise a node now, so calling one to find out whether there
        // is one would fail rather than answer (#71).
        return Services.getInstance(p, ProjectIndexer.class).nodeExists(file.getDir().getPath());
    }

    /**
     * Opens a node's editor - or focuses the one already open - and hands it to
     * whoever asked, once there is one to hand.
     * <p>
     * <b>One block and in order.</b> {@code openNow} blocks until the editor
     * exists, so what follows has something to talk to. It cannot be a second
     * {@code invokeLater}: the open pumps the event queue while it waits, so the
     * second call runs <em>inside</em> the first and finds no editor - which is
     * why a test case picked from the search opened its editor on page one with
     * nothing selected (#29). Owning that ordering in one place is what this
     * method is for, and why the two halves it uses stay private.
     * <p>
     * <b>What to say to the editor is the caller's.</b> A run says "start what
     * you have left", a search result says "land on this case"; neither is
     * knowledge a class named after editors should be carrying, and each lives
     * in the package the sentence is about.
     */
    public void openThen(final @NotNull Project p, final @NotNull DirectoryDto dir, final @NotNull Consumer<TestinEditor> tell) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!openNow(p, dir)) return;

            editorFor(p, dir).ifPresent(tell);
        });
    }

    /**
     * Opens a node's editor and puts the cursor on one of its test cases (#29).
     * <p>
     * Told to the editor rather than done to it: an editor that has just been
     * built has no test cases yet - it reads them on a pooled thread - so it is
     * asked to land on the case once it has one.
     *
     * @param tc the case to land on inside that editor
     */
    public void openAndSelect(final @NotNull Project p, final @NotNull DirectoryDto dir, final @NotNull TestCaseDto tc) {
        openThen(p, dir, editor -> {
            editor.selectWhenLoaded(tc.getId());

            // Outside the editor's own business: the details panel is handed the
            // case itself rather than asked to find it, so it fills in whether
            // the editor has read its cases or not.
            ViewToolWindowFactory.showPanel(p, List.of(tc), dir.getPath2());
        });
    }

    /**
     * The open Testin editor showing this node, and empty when none is - which
     * happens when the open above could not build one.
     */
    private @NotNull Optional<TestinEditor> editorFor(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(p);

        for (final VirtualFile open : fed.getOpenFiles()) {
            if (!(open instanceof UnifiedVirtualFile testinFile)) continue;
            if (!testinFile.getDir().getPath().equals(dir.getPath())) continue;

            for (final FileEditor tab : fed.getAllEditors(testinFile)) {
                if (tab instanceof UnifiedFileEditor unified) return Optional.of(unified.getEditor());
            }
        }

        return Optional.empty();
    }

    public void closeThenOpen(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(p);

        ApplicationManager.getApplication().invokeLater(() -> {
            Optional<VirtualFile> targetVf = Optional.empty();

            for (final VirtualFile openVf : fed.getOpenFiles()) {
                // By path, not by name: two nodes in different packages can share
                // a name, and matching on it would close a neighbour's editor
                // instead of this node's. The path is the node's identity, the
                // same match openNow and editorFor use.
                if (!(openVf instanceof UnifiedVirtualFile testinFile)) continue;
                if (!testinFile.getDir().getPath().equals(dir.getPath())) continue;

                targetVf = Optional.of(openVf);
                fed.closeFile(openVf);
                break;
            }

            if (targetVf.isEmpty()) {
                open(p, dir);
                return;
            }

            fed.openFile(targetVf.orElseThrow(), true);
        });
    }

    /**
     * Opens the node's editor, or focuses it when it is already open.
     * <p>
     * There used to be an {@code openIfNotOpen} beside this saying the same
     * thing, from when this one always opened a second tab. Opening what is
     * already open <em>is</em> focusing it, so the two names were one behavior.
     */
    public void open(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        ApplicationManager.getApplication().invokeLater(() -> openNow(p, dir));
    }

    /**
     * Opens the node's editor, or focuses it when it is already open, and
     * answers whether there is now an editor to talk to.
     * <p>
     * <b>On the EDT, and synchronous.</b> {@code openFile} blocks until the
     * editor exists, so a caller that needs to say something to that editor can
     * say it on the next line. It cannot say it from a second
     * {@code invokeLater}: openFile pumps the event queue while it waits, so the
     * second call runs <em>inside</em> the first and finds no editor yet. That
     * is why a test case picked from the search opened its editor on page one
     * with nothing selected, while one whose editor was already open worked
     * (#29).
     * <p>
     * A node with no editor is refused rather than guessed at. The type used to
     * be read as "a test run, or else a test set", so every other node - a
     * package, the Test Cases folder, the Test Runs folder - was opened as a
     * test set and died casting itself to one. What can be opened is the node's
     * own declaration, and the two kinds that say yes are exactly the two the
     * editors are written for.
     */
    private boolean openNow(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(p);

        for (final VirtualFile open : fed.getOpenFiles()) {
            // By path, not by name: two nodes in different packages can share a
            // name, and matching on the name focused whichever one happened to be
            // open first - so a case picked from the search opened its neighbour's
            // editor. The path is the node's identity, which is what editorFor
            // already matches on.
            if (!(open instanceof UnifiedVirtualFile testinFile)) continue;
            if (!testinFile.getDir().getPath().equals(dir.getPath())) continue;

            Logger.info("Editor already open, focusing: " + dir.getName());
            fed.openFile(open, true);
            return true;
        }

        if (!dir.isOpenableInEditor()) {
            Logger.info("Nothing to open for " + dir.getName() + " - it holds nodes rather than test cases");
            return false;
        }

        Logger.info("Opening Editor: " + dir.getPath());
        final @NotNull EditorType ft = dir instanceof TestRunDirectoryDto ? EditorType.TEST_RUN : EditorType.TEST_CASE;
        fed.openFile(new UnifiedVirtualFile(dir, ft), true);

        return true;
    }

    public void saveOpen(final @NotNull Project p) {
        try {
            final @NotNull FileEditorManager fileEditorManager = FileEditorManager.getInstance(p);
            final @NotNull List<String> entries = getEntries(fileEditorManager);

            if (entries.isEmpty())
                PropertiesComponent.getInstance(p).setValue(OPEN_EDITORS_KEY, null);
            else
                PropertiesComponent.getInstance(p).setValue(OPEN_EDITORS_KEY, String.join(";", entries));

        } catch (final Exception ex) {
            Logger.error("Failed to save open editors: " + ex.getMessage());
        }
    }

    /**
     * A path per open editor, and nothing else. The entry used to carry a "ts"
     * or "tr" prefix saying which kind of node it was, which the restore parsed
     * back to pick a lookup.
     * <p>
     * The indexer finds a node of any kind by path, and {@link #open} already
     * decides the editor type from the node's own class. The prefix said nothing
     * the path did not.
     */
    private @NotNull List<String> getEntries(final @NotNull FileEditorManager fed) {
        final @NotNull List<String> entries = new ArrayList<>();

        for (final VirtualFile vf : fed.getOpenFiles()) {
            if (vf instanceof UnifiedVirtualFile uvf) {
                entries.add(uvf.getDir().getPath().toAbsolutePath().toString());
            }
        }
        return entries;
    }

    public void restoreLastOpened(final @NotNull Project p) {
        try {
            final @NotNull String saved = Objects.requireNonNullElse(
                    PropertiesComponent.getInstance(p).getValue(OPEN_EDITORS_KEY), "");

            if (saved.isEmpty()) {
                Logger.debug("EditorStateService: no saved editors to restore");
                return;
            }

            final String @NotNull[] entries = saved.split(";");
            if (entries.length == 0)
                return;

            Logger.info("restoring " + entries.length + " open editors");

            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

            for (final String entry : entries) {
                // A remembered editor whose node is not there any more is not
                // reopened: the path was written last time the project closed and
                // the node may have been removed since.
                indexer.find(Path.of(entry)).ifPresentOrElse(
                        dir -> {
                            Logger.debug("restoring editor for '" + entry + "' -> found");
                            open(p, dir);
                        },
                        () -> Logger.debug("restoring editor for '" + entry + "' -> not indexed"));
            }

            PropertiesComponent.getInstance(p).setValue(OPEN_EDITORS_KEY, null);
            Logger.info("EditorStateService: cleared saved editor state");

        } catch (final Exception ex) {
            Logger.error("Failed to restore open editors: " + ex.getMessage());
        }
    }

}