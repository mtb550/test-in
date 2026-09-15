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
 * Also the one place a pasted PNG is read back into a picture: the thumbnail
 * under the error box asks {@link #read} too, so a screenshot that draws in one
 * draws in the other.
 */
public final class Picture implements DialogComponent {

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
