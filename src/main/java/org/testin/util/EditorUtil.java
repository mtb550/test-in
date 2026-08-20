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
import org.testin.editor.UnifiedFileEditor;
import org.testin.editor.UnifiedVirtualFile;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class EditorUtil {
    private final @NotNull String OPEN_EDITORS_KEY = "testin.openEditors";

    public boolean isOpen(final @NotNull Project p, final @NotNull String s) {
        final FileEditorManager fed = FileEditorManager.getInstance(p);
        final VirtualFile[] openFiles = fed.getOpenFiles();

        for (final VirtualFile vf : openFiles) {
            if (s.equals(vf.getName())) {
                fed.openFile(vf, true);
                return true;
            }
        }

        return false;
    }

    public void close(final @NotNull Project p, final @NotNull String s) {
        final FileEditorManager fed = FileEditorManager.getInstance(p);
        final VirtualFile[] openFiles = fed.getOpenFiles();

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
        final FileEditorManager fed = FileEditorManager.getInstance(p);

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

    public void closeThenOpen(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        final FileEditorManager fed = FileEditorManager.getInstance(p);

        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile targetVf = null;

            for (final VirtualFile openVf : fed.getOpenFiles()) {
                if (openVf.getName().equals(dir.getName())) {
                    targetVf = openVf;
                    fed.closeFile(openVf);
                    break;
                }
            }

            if (targetVf == null) {
                open(p, dir);
                return;
            }

            fed.openFile(targetVf, true);
        });
    }

    public void open(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        final EditorType ft = dir instanceof TestRunDirectoryDto ? EditorType.TEST_RUN : EditorType.TEST_CASE;
        final UnifiedVirtualFile newVf = new UnifiedVirtualFile(dir, ft);

        ApplicationManager.getApplication().invokeLater(() ->
                Optional.ofNullable(FileEditorManager.getInstance(p))
                        .ifPresent(editorManager -> editorManager.openFile(newVf, true)));
    }

    public void openIfNotOpen(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        if (isOpen(p, dir.getName())) {
            Logger.info("Editor already open, focusing: " + dir.getName());

        } else {
            Logger.info("Opening Editor: " + dir.getPath());
            open(p, dir);
        }
    }

    public void saveOpen(final @NotNull Project p) {
        try {
            final FileEditorManager fileEditorManager = FileEditorManager.getInstance(p);
            final List<String> entries = getEntries(fileEditorManager);

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
        final List<String> entries = new ArrayList<>();

        for (final VirtualFile vf : fed.getOpenFiles()) {
            if (vf instanceof UnifiedVirtualFile uvf) {
                entries.add(uvf.getDir().getPath().toAbsolutePath().toString());
            }
        }
        return entries;
    }

    public void restoreLastOpened(final @NotNull Project p) {
        try {
            final String saved = PropertiesComponent.getInstance(p).getValue(OPEN_EDITORS_KEY);

            if (saved == null || saved.isEmpty()) {
                Logger.debug("EditorStateService: no saved editors to restore");
                return;
            }

            final String[] entries = saved.split(";");
            if (entries.length == 0)
                return;

            Logger.info("restoring " + entries.length + " open editors");

            final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

            for (final String entry : entries) {
                // A remembered editor whose node is not there any more is not
                // reopened: the path was written last time the project closed and
                // the node may have been removed since.
                indexer.find(Path.of(entry)).ifPresentOrElse(
                        dir -> {
                            Logger.debug("restoring editor for '" + entry + "' -> found");
                            openIfNotOpen(p, dir);
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