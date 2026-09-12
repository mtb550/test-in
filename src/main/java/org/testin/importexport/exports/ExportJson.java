/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.importexport.exports;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class ExportJson {

    /**
     * UC-SHARE-001, Rule-SHARE-010.
     * <p>
     * Writes the export itself rather than borrowing the indexer's writer: an
     * export lands outside the test data tree, on a destination the tester chose,
     * so it is not the indexer's to own. {@code importexport} is an exempt
     * package for exactly this.
     */
    public void exportToFile(final @NotNull Project p, final @NotNull File destFile, final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        try {
            // The platform's own helper: it knows that a bare file name has no
            // parent directory to create, so nothing here has to.
            FileUtil.createParentDirs(destFile);

            Files.write(destFile.toPath(), Services.getInstance(p, Mapper.class).writeValueAsBytes(sheetsData));
        } catch (final IOException ex) {
            // Raised, not reported and swallowed, which is what the other three
            // exporters do. Returning here left the caller to carry on and show
            // "Exported 200" under the failure it had just raised, so only the
            // JSON export told the tester both things about one attempt (#66,
            // finding 81). The one owner above catches it and says it once.
            Logger.error("export failed: " + destFile + " - " + ex.getMessage());
            throw new RuntimeException(ex);
        }

        ExportNotice.show(p, destFile);
    }
}
