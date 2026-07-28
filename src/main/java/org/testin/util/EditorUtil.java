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
import org.jspecify.annotations.NonNull;
import org.testin.editorPanel.EditorType;
import org.testin.editorPanel.UnifiedVirtualFile;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class EditorUtil {
    private final String OPEN_EDITORS_KEY = "testin.openEditors";

    public boolean isEditorOpen(final @NotNull Project project, final String s) {
        final FileEditorManager editorManager = FileEditorManager.getInstance(project);
        final VirtualFile[] openFiles = editorManager.getOpenFiles();

        for (VirtualFile vf : openFiles) {
            if (s.equals(vf.getName())) {
                editorManager.openFile(vf, true);
                return true;
            }
        }

        return false;
    }

    public void closeEditor(final @NotNull Project project, final String s) {
        final FileEditorManager editorManager = FileEditorManager.getInstance(project);
        final VirtualFile[] openFiles = editorManager.getOpenFiles();

        for (VirtualFile vf : openFiles) {
            if (s.equals(vf.getName())) {
                editorManager.closeFile(vf);
                break;
            }
        }

    }

    public void closeThenOpenEditor(final @NotNull Project project, final VirtualFile vf, final DirectoryDto dir) {
        if (vf == null || dir == null) return;
        final FileEditorManager editorManager = FileEditorManager.getInstance(project);

        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile targetVf = null;

            for (VirtualFile openVf : editorManager.getOpenFiles()) {
                if (openVf.getName().equals(vf.getName())) {
                    targetVf = openVf;
                    editorManager.closeFile(openVf);
                    break;
                }
            }

            if (targetVf == null) {
                openEditor(project, dir);
                return;
            }

            editorManager.openFile(targetVf, true);
        });
    }

    public void openEditor(final @NotNull Project project, final DirectoryDto dir) {
        final EditorType ft = dir instanceof TestRunDirectoryDto ? EditorType.TEST_RUN : EditorType.TEST_CASE;
        final UnifiedVirtualFile newVf = new UnifiedVirtualFile(dir, ft);

        ApplicationManager.getApplication().invokeLater(() ->
                Optional.ofNullable(FileEditorManager.getInstance(project))
                        .ifPresent(editorManager -> editorManager.openFile(newVf, true)));
    }

    public void openEditorIfNotOpen(final @NotNull Project project, final DirectoryDto dir) {
        if (isEditorOpen(project, dir.getName())) {
            Logger.info("Editor already open, focusing: " + dir.getName());

        } else {
            Logger.info("Opening Editor: " + dir.getPath());
            openEditor(project, dir);
        }
    }

    public void saveOpenEditors(final @NotNull Project project) {
        try {
            final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            final List<String> entries = getEntries(fileEditorManager);

            if (entries.isEmpty())
                PropertiesComponent.getInstance(project).setValue(OPEN_EDITORS_KEY, null);
            else
                PropertiesComponent.getInstance(project).setValue(OPEN_EDITORS_KEY, String.join(";", entries));

        } catch (final Exception ex) {
            Logger.error("Failed to save open editors: " + ex.getMessage());
        }
    }

    private @NonNull List<String> getEntries(final FileEditorManager fileEditorManager) {
        final VirtualFile[] openFiles = fileEditorManager.getOpenFiles();

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

    public void restoreOpenEditors(final @NotNull Project project) {
        try {
            final String saved = PropertiesComponent.getInstance(project).getValue(OPEN_EDITORS_KEY);

            if (saved == null || saved.isEmpty()) {
                Logger.debug("EditorStateService: no saved editors to restore");
                return;
            }

            final String[] entries = saved.split(";");
            if (entries.length == 0)
                return;

            Logger.info("restoring " + entries.length + " open editors");

            final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);

            for (final String entry : entries) {
                final int sep = entry.indexOf('|');
                final String type = entry.substring(0, sep);
                final Path path = Path.of(entry.substring(sep + 1));

                if ("ts".equals(type)) {
                    final TestSetDirectoryDto ts = indexer.getTestSetByPath(path);
                    Logger.debug("EditorStateService: lookup testSet by path '" + path + "' -> " + "found");

                    openEditorIfNotOpen(project, ts);

                } else if ("tr".equals(type)) {
                    final TestRunDirectoryDto tr = indexer.getTestRunDirByPath(path);
                    Logger.debug("EditorStateService: lookup testRun by path '" + path + "' -> " + "found");
                    openEditorIfNotOpen(project, tr);

                } else {
                    Logger.warn("EditorStateService: unknown directory type '" + type + "', skipping: " + path);
                }
            }

            PropertiesComponent.getInstance(project).setValue(OPEN_EDITORS_KEY, null);
            Logger.info("EditorStateService: cleared saved editor state");

        } catch (final Exception ex) {
            Logger.error("Failed to restore open editors: " + ex.getMessage());
        }
    }

}