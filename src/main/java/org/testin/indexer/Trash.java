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

package org.testin.indexer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Once;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class Trash {
    private static final @NotNull Key<Boolean> NO_BIN_SAID = Key.create("testin.trash.noBinSaid");

    // UC-INTERNAL-005, Rule-INTERNAL-036
    static boolean accepted(final @NotNull Project p, final @NotNull Path path) {
        if (!Files.exists(path) || ApplicationManager.getApplication().isUnitTestMode()) return false;

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            Logger.debug("This desktop has no recycle bin; deleting " + path + " outright.");
            sayThereIsNoBin(p);
            return false;
        }

        try {
            if (Desktop.getDesktop().moveToTrash(path.toFile())) return true;

            Logger.warn("The recycle bin refused " + path + ", deleting it instead.");
            sayThereIsNoBin(p);
            return false;

        } catch (final Exception ex) {
            Logger.warn("Could not move " + path + " to the recycle bin, deleting it instead: " + ex.getMessage());
            sayThereIsNoBin(p);
            return false;
        }
    }

    // UC-INTERNAL-005, Rule-INTERNAL-036
    private static void sayThereIsNoBin(final @NotNull Project p) {
        if (!Once.claim(p, NO_BIN_SAID)) return;

        Services.getInstance(p, Notifier.class).info(p, Bundle.message("trash.none.title"), Bundle.message("trash.none.message"));
    }
}
