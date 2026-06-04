package org.testin.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.FileType;
import org.testin.editorPanel.UnifiedVirtualFile;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.util.logger.Log;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EditorUtil {

    private static final EditorUtil INSTANCE = new EditorUtil();

    public static EditorUtil getInstance() {
        return INSTANCE;
    }

    public boolean isEditorOpen(final @NotNull Project project, final String s) {
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        VirtualFile[] openFiles = editorManager.getOpenFiles();

        for (VirtualFile vf : openFiles) {
            if (s.equals(vf.getName())) {
                editorManager.openFile(vf, true);
                return true;
            }
        }

        return false;
    }

    public void closeEditor(final @NotNull Project project, final String s) {
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        VirtualFile[] openFiles = editorManager.getOpenFiles();

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
        final FileType ft = dir instanceof TestRunDirectoryDto ? FileType.TEST_RUN : FileType.TEST_CASE;
        final UnifiedVirtualFile newVf = new UnifiedVirtualFile(dir, ft);

        ApplicationManager.getApplication().invokeLater(() ->
                Optional.ofNullable(FileEditorManager.getInstance(project))
                        .ifPresent(editorManager -> editorManager.openFile(newVf, true))
        );
    }

    public void openEditorIfNotOpen(final @NotNull Project project, final DirectoryDto dir) {
        if (isEditorOpen(project, dir.getName())) {
            Log.info("Editor already open, focusing: " + dir.getName());
        } else {
            Log.info("Opening Editor: " + dir.getPath());
            openEditor(project, dir);
        }
    }
}