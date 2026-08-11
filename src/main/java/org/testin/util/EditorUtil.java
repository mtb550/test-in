package org.testin.util;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.testin.editorPanel.EditorType;
import org.testin.editorPanel.UnifiedVirtualFile;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class EditorUtil {
    private final String OPEN_EDITORS_KEY = "testin.openEditors";

    public boolean isOpen(final @NotNull Project p, final @Nullable String s) {
        final FileEditorManager fed = FileEditorManager.getInstance(p);
        final VirtualFile[] openFiles = fed.getOpenFiles();

        for (VirtualFile vf : openFiles) {
            if (s != null && s.equals(vf.getName())) {
                fed.openFile(vf, true);
                return true;
            }
        }

        return false;
    }

    public void close(final @NotNull Project p, final @NotNull String s) {
        final FileEditorManager fed = FileEditorManager.getInstance(p);
        final VirtualFile[] openFiles = fed.getOpenFiles();

        for (VirtualFile vf : openFiles) {
            if (s.equals(vf.getName())) {
                fed.closeFile(vf);
                break;
            }
        }

    }

    public void closeThenOpen(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        final FileEditorManager fed = FileEditorManager.getInstance(p);

        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile targetVf = null;

            for (VirtualFile openVf : fed.getOpenFiles()) {
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

    public void openIfNotOpen(final @NotNull Project p, final @Nullable DirectoryDto dir) {
        if (dir == null) return;

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

    private @NonNull List<String> getEntries(final @NotNull FileEditorManager fed) {
        final VirtualFile[] openFiles = fed.getOpenFiles();

        final List<String> entries = new ArrayList<>();
        for (final VirtualFile vf : openFiles) {
            if (vf instanceof UnifiedVirtualFile uvf) {
                final Path dirPath = uvf.getDir().getPath();
                final String pathStr = dirPath.toAbsolutePath().toString();
                final String type = uvf.getFileType() == EditorType.TEST_RUN ? "tr" : "ts";

                entries.add(type + "|" + pathStr);
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
                final int sep = entry.indexOf('|');
                final String type = entry.substring(0, sep);
                final Path path = Path.of(entry.substring(sep + 1));

                if ("ts".equals(type)) {
                    final TestSetDirectoryDto ts = indexer.getTestSetByPath(path);
                    Logger.debug("EditorStateService: lookup testSet by path '" + path + "' -> " + "found");

                    openIfNotOpen(p, ts);

                } else if ("tr".equals(type)) {
                    final TestRunDirectoryDto tr = indexer.getTestRunDirByPath(path);
                    Logger.debug("EditorStateService: lookup testRun by path '" + path + "' -> " + "found");
                    openIfNotOpen(p, tr);

                } else {
                    Logger.warn("EditorStateService: unknown directory type '" + type + "', skipping: " + path);
                }
            }

            PropertiesComponent.getInstance(p).setValue(OPEN_EDITORS_KEY, null);
            Logger.info("EditorStateService: cleared saved editor state");

        } catch (final Exception ex) {
            Logger.error("Failed to restore open editors: " + ex.getMessage());
        }
    }

}