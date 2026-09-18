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

package org.testin.ui.framework;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBImageIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * A screenshot at its real size, scrolled when it is larger than the dialog
 * (#50).
 * <p>
 * Also the one place a PNG is read back into a picture, and the one maker of
 * its thumbnail: the failure form's strip under the error box and the details
 * panel's Stacktrace row both ask {@link #thumbnail}, so a screenshot that
 * draws in one draws the same in the other.
 */
public final class Picture implements DialogComponent {

    /**
     * How high a screenshot's thumbnail is drawn, wherever one is.
     */
    private static final int THUMBNAIL_HEIGHT = 48;

    private final @NotNull JBScrollPane panel;

    Picture(final byte @NotNull [] png) {
        panel = new JBScrollPane(new JBLabel(read(png).<Icon>map(JBImageIcon::new).orElse(EmptyIcon.ICON_0)));
        panel.setBorder(JBUI.Borders.empty());
    }

    /**
     * The picture these bytes hold, and empty for bytes that are not one - only
     * a hand-edited run file holds those. Logged rather than thrown: a
     * screenshot that cannot be drawn still stays saved.
     */
    static @NotNull Optional<BufferedImage> read(final byte @NotNull [] png) {
        try {
            final @NotNull Optional<BufferedImage> image = Optional.ofNullable(ImageIO.read(new ByteArrayInputStream(png)));
            if (image.isEmpty()) Logger.warn("A stored screenshot is not a picture; it stays saved and is drawn empty");
            return image;
        } catch (final IOException ex) {
            Logger.warn("A stored screenshot could not be read, so it is drawn empty: " + ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * UC-VIEW-PANEL-006, Rule-VIEW-PANEL-081.
     * <p>
     * The screenshot at thumbnail height, its width in proportion, and the empty
     * square of {@link #noThumbnail} for one that cannot be read.
     * <p>
     * Moved here from the failure form's strip, where it was private, so the
     * details panel draws the same picture rather than a second maker of it
     * (#328).
     */
    public static @NotNull Icon thumbnail(final byte @NotNull [] png) {
        return read(png)
                .<Icon>map(image -> new JBImageIcon(ImageUtil.scaleImage(image, JBUI.scale(THUMBNAIL_HEIGHT) / (double) image.getHeight())))
                .orElseGet(Picture::noThumbnail);
    }

    /**
     * The empty square a thumbnail takes while it cannot be drawn: before its
     * file has been read, and for bytes that are not a picture. The same size,
     * so nothing moves when the picture arrives.
     */
    public static @NotNull Icon noThumbnail() {
        return EmptyIcon.create(JBUI.scale(THUMBNAIL_HEIGHT));
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return panel;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        // Looking at a picture is not a submit gesture; the declared keys close.
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }
}
