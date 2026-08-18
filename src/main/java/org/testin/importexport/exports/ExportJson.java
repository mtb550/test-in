package org.testin.importexport.exports;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
            final @Nullable Path parent = destFile.toPath().getParent();
            if (parent != null) Files.createDirectories(parent);

            Files.write(destFile.toPath(), Services.getInstance(p, Mapper.class).writeValueAsBytes(sheetsData));
        } catch (final IOException ex) {
            Services.getInstance(p, Notifier.class).error(p, "Export failed: " + ex.getMessage());
            Logger.error("export failed: " + destFile + " - " + ex.getMessage());
            return;
        }

        ExportNotice.show(p, destFile);
    }
}
