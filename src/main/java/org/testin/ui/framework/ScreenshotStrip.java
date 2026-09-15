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

import com.intellij.icons.AllIcons;
import com.intellij.ui.InplaceButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBImageIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The screenshots pasted into a text area: a small picture of each, and the
 * button that takes it out (#50).
 * <p>
 * Built only from platform parts - a label holding a scaled icon and the
 * platform's own close button - so nothing here paints, and nothing breaks when
 * a theme, a font or a screen changes. Loading and pasting both go through
 * {@link #add}, so a screenshot cannot look one way when pasted and another when
 * the form is opened again.
 */
final class ScreenshotStrip {

    private static final int HEIGHT = 48;

    private final @NotNull List<byte[]> screenshots = new ArrayList<>();

    /**
     * No vertical gap, so a strip with no thumbnails takes no height under a box
     * that has never held a screenshot.
     */
    private final @NotNull JBPanel<?> panel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0));

    ScreenshotStrip(final @NotNull List<byte[]> stored) {
        panel.setOpaque(false);
        stored.forEach(this::add);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    void add(final byte @NotNull [] png) {
        final @NotNull JBPanel<?> thumbnail = new JBPanel<>(new BorderLayout());
        thumbnail.setOpaque(false);
        thumbnail.add(new JBLabel(thumbnailOf(png)), BorderLayout.CENTER);
        thumbnail.add(new InplaceButton(Bundle.message("dialog.failure.screenshot.remove"), AllIcons.Actions.Close, click -> remove(png, thumbnail)), BorderLayout.EAST);

        screenshots.add(png);
        panel.add(thumbnail);
        panel.revalidate();
    }

    /**
     * A copy, so nothing outside can change the list the thumbnails show.
     */
    @NotNull List<byte[]> screenshots() {
        return List.copyOf(screenshots);
    }

    @NotNull JComponent getPanel() {
        return panel;
    }

    /**
     * UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219.
     * <p>
     * That exact screenshot: each paste is its own array, and the list removes
     * by identity, so one of two identical screenshots goes and the other stays.
     */
    private void remove(final byte @NotNull [] png, final @NotNull JComponent thumbnail) {
        screenshots.remove(png);
        panel.remove(thumbnail);
        panel.revalidate();
        panel.repaint();
    }

    /**
     * The screenshot at thumbnail height, its width in proportion, and an empty
     * square for one that cannot be read.
     */
    private static @NotNull Icon thumbnailOf(final byte @NotNull [] png) {
        return Picture.read(png)
                .<Icon>map(image -> new JBImageIcon(ImageUtil.scaleImage(image, JBUI.scale(HEIGHT) / (double) image.getHeight())))
                .orElseGet(() -> EmptyIcon.create(JBUI.scale(HEIGHT)));
    }
}
