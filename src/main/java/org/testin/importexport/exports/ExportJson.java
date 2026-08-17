package org.testin.importexport.exports;

import com.intellij.notification.NotificationAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Mapper;
import org.testin.util.Tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class ExportJson {

    /**
     * Writes the export itself rather than borrowing the indexer's writer: an
     * export lands outside the test data tree, on a destination the tester chose,
     * so it is not the indexer's to own. {@code importexport} is an exempt
     * package for exactly this.
     */
    public void exportToFile(final @NotNull Project p, final @NotNull File destFile,
                             final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        try {
            Files.write(destFile.toPath(), Services.getInstance(p, Mapper.class).writeValueAsBytes(sheetsData));
        } catch (final IOException ex) {
            Services.getInstance(p, Notifier.class).error(p, "Export failed: " + ex.getMessage());
            Logger.error("export failed: " + destFile + " - " + ex.getMessage());
            return;
        }

        ApplicationManager.getApplication().invokeLater(() ->
                Services.getInstance(p, Notifier.class).infoWithActions(p, "Export Complete", "Exported to: " + destFile.getName(),
                        NotificationAction.createSimple("Open file", () -> {
                            final VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(destFile.getAbsolutePath());
                            Services.getInstance(p, Tools.class).openWithAssociatedProgram(p, vf);
                        }))
        );
    }
}
