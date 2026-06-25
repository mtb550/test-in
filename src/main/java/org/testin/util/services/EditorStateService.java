package org.testin.util.services;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.FileType;
import org.testin.editorPanel.UnifiedVirtualFile;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class EditorStateService {

    private static final String OPEN_EDITORS_KEY = "testin.openEditors";

    private final Project project;

    public EditorStateService(final @NotNull Project project) {
        this.project = project;

        project.getMessageBus().connect(project)
                .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
                        new FileEditorManagerListener() {
                            @Override
                            public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                                if (file instanceof UnifiedVirtualFile) {
                                    saveOpenEditors();
                                }
                            }
                        });
    }

    public void saveOpenEditors() {
        final FileEditorManager editorManager = FileEditorManager.getInstance(project);
        final VirtualFile[] openFiles = editorManager.getOpenFiles();
        final List<String> paths = new ArrayList<>();

        for (final VirtualFile vf : openFiles) {
            if (vf instanceof UnifiedVirtualFile uvf) {
                final Path dirPath = uvf.getDir().getPath();
                paths.add(dirPath.toAbsolutePath().toString());
            }
        }

        if (paths.isEmpty()) {
            PropertiesComponent.getInstance().setValue(OPEN_EDITORS_KEY, null);
        } else {
            PropertiesComponent.getInstance().setValue(OPEN_EDITORS_KEY, String.join("|", paths));
        }

        Log.info("EditorStateService: saved " + paths.size() + " open editors");
    }

    public void restoreOpenEditors() {
        final String saved = PropertiesComponent.getInstance().getValue(OPEN_EDITORS_KEY);
        if (saved == null || saved.isEmpty()) return;

        final String[] pathStrings = saved.split("\\|");
        if (pathStrings.length == 0) return;

        Log.info("EditorStateService: restoring " + pathStrings.length + " open editors");

        final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);

        for (final String pathStr : pathStrings) {
            final Path dirPath = Path.of(pathStr);

            DirectoryDto dir = indexer.getTestSetByPath(dirPath);
            if (dir == null) {
                dir = indexer.getTestRunDirByPath(dirPath);
            }

            if (dir != null) {
                final FileType ft = dir instanceof TestRunDirectoryDto
                        ? FileType.TEST_RUN : FileType.TEST_CASE;
                final UnifiedVirtualFile vf = new UnifiedVirtualFile(dir, ft);
                final FileEditorManager editorManager = FileEditorManager.getInstance(project);
                if (editorManager != null) {
                    editorManager.openFile(vf, true);
                }
            } else {
                Log.warn("EditorStateService: directory not found in indexer: " + pathStr);
            }
        }
    }

}
