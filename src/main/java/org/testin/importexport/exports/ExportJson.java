package org.testin.importexport.exports;

import com.intellij.notification.NotificationAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.FilesUtil;
import org.testin.util.Tools;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ExportJson {

    public void exportToFile(final @NotNull Project p, final @NotNull File destFile,
                             final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        Services.getInstance(p, FilesUtil.class).write(p, destFile.toPath(), sheetsData);

        ApplicationManager.getApplication().invokeLater(() ->
                Services.getInstance(p, Notifier.class).infoWithActions(p, "Export Complete", "Exported to: " + destFile.getName(),
                        NotificationAction.createSimple("Open file", () -> {
                            final VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(destFile.getAbsolutePath());
                            Services.getInstance(p, Tools.class).openWithAssociatedProgram(p, vf);
                        }))
        );
    }
}
