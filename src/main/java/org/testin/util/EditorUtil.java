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

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class EditorUtil {
    private final @NotNull String OPEN_EDITORS_KEY = "testin.openEditors";

    public boolean isOpen(final @NotNull Project p, final @NotNull String s) {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(p);
        final VirtualFile @NotNull[] openFiles = fed.getOpenFiles();

        for (final VirtualFile vf : openFiles) {
            if (s.equals(vf.getName())) {
                fed.openFile(vf, true);
                return true;
            }
        }

        return false;
    }

    public void close(final @NotNull Project p, final @NotNull String s) {
        final @NotNull FileEditorManager fed = FileEditorManager.getInstance(p);
        final VirtualFile @NotNull[] openFiles = fed.getOpenFiles();

        for (final VirtualFile vf : openFiles) {
            if (s.equals(vf.getName())) {
                fed.closeFile(vf);
                break;
            }
        }

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
                if (tab instanceof UnifiedFileEditor unified) unified.getEditor().reload();
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
     * Opens a node's editor and puts the cursor on one of its test cases (#29).
     * <p>
     * The two halves are separate because opening is asynchronous: the editor is
     * built when the platform gets round to it, so the case cannot be selected in
     * the same breath. Waiting for the editor to appear rather than assuming it
     * has is why this is here and not written out at the call site - the create
     * action does the same thing today by holding the editor it already had, and
     * a caller that only has a node has nothing to hold.
     *
     * @param tc the case to land on inside that editor
     */
    public void openAndSelect(final @NotNull Project p, final @NotNull DirectoryDto dir,
                              final @NotNull TestCaseDto tc) {
        ApplicationManager.getApplication().invokeLater(() -> {
            // One block and in order, because openNow blocks until the editor
            // exists - so the next line has something to talk to. Two separate
            // invokeLater calls did not work: the open pumps the event queue
            // while it waits, and the second call ran inside the first.
            if (!openNow(p, dir)) return;

            // Told to the editor rather than done to it: an editor that has just
            // been built has no test cases yet - it reads them on a pooled
            // thread - so it is asked to land on the case once it has one.
            editorFor(p, dir).ifPresent(editor -> editor.selectWhenLoaded(tc.getId()));

            // Outside the editor's own business, and it works either way: the
            // details panel is handed the case itself rather than asked to find
            // it, so it fills in whether the editor is ready or not.
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
                if (openVf.getName().equals(dir.getName())) {
                    targetVf = Optional.of(openVf);
                    fed.closeFile(openVf);
                    break;
                }
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