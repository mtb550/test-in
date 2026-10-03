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

package org.testin.report.generators;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.ui.framework.Picture;
import org.testin.util.FailureText;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
final class CompanyLogo {
    static final float HEIGHT_PT = 30;

    static final int HEIGHT_PX = 40;

    private final @NotNull BufferedImage image;

    // UC-SETTING-012, Rule-SETTING-044
    static @NotNull Optional<CompanyLogo> fromSettings() {
        final @NotNull String chosen = Services.getInstance(AppSettingsState.class).companyLogo;
        if (chosen.isEmpty()) return Optional.empty();

        try {
            final @NotNull Path file = Path.of(chosen);
            if (!Files.isRegularFile(file)) {
                Logger.info("The company logo is not a file, so the report has none: " + chosen);
                return Optional.empty();
            }

            final @NotNull Optional<BufferedImage> read = Optional.ofNullable(ImageIO.read(file.toFile()));
            if (read.isEmpty()) Logger.info("The company logo is not a picture, so the report has none: " + chosen);
            return read.map(CompanyLogo::new);
        } catch (final InvalidPathException | IOException ex) {
            Logger.warn("The company logo could not be read, so the report has none: " + FailureText.of(ex));
            return Optional.empty();
        }
    }

    byte @NotNull [] png() {
        return Picture.toPng(image);
    }

    // Rule-SETTING-043
    float widthAt(final float height) {
        return height * image.getWidth() / image.getHeight();
    }
}
