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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.ui.Caption;
import org.testin.util.ClipboardContents;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

// UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
public final class Screenshots implements DialogComponent {
    private final @NotNull ScreenshotStrip strip;
    private final @NotNull BorderLayoutPanel panel;

    private @NotNull Runnable changed = () -> {
    };

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219, Rule-INTERNAL-087
    Screenshots(final @NotNull String caption, final @NotNull List<byte[]> stored) {
        strip = new ScreenshotStrip(stored);
        panel = Caption.above(caption, strip.getPanel());

        strip.onChange(this::redraw);
        panel.setVisible(!strip.screenshots().isEmpty());
    }

    private static byte @NotNull [] toPng(final @NotNull Image image) {
        final BufferedImage buffered;
        if (image instanceof BufferedImage alreadyBuffered) {
            buffered = alreadyBuffered;
        } else {
            final int width = image.getWidth(null);
            final int height = image.getHeight(null);
            if (width <= 0 || height <= 0) return new byte[0];

            buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            final @NotNull Graphics2D g = buffered.createGraphics();
            try {
                g.drawImage(image, 0, 0, null);
            } finally {
                g.dispose();
            }
        }

        try {
            final @NotNull ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(buffered, "png", out);
            return out.toByteArray();
        } catch (final IOException ex) {
            Logger.error("Could not encode a pasted image as PNG: " + ex.getMessage());
            return new byte[0];
        }
    }

    private static boolean onClipboard() {
        return ClipboardContents.withFlavor(DataFlavor.imageFlavor).isPresent();
    }

    public @NotNull List<byte[]> pictures() {
        return strip.screenshots();
    }

    public void onChange(final @NotNull Runnable changed) {
        this.changed = changed;
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return strip.getPanel();
    }

    @Override
    public boolean wantsFocus() {
        return false;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    @Override
    public void installedOn(final @NotNull JComponent root) {
        new Paste().registerCustomShortcutSet(CommonShortcuts.getPaste(), root);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    private void add(final @NotNull Transferable contents) {
        try {
            final byte @NotNull [] png = toPng((Image) contents.getTransferData(DataFlavor.imageFlavor));
            if (png.length > 0) strip.add(png);
        } catch (final UnsupportedFlavorException | IOException ex) {
            Logger.warn("Could not read the pasted image: " + ex.getMessage());
        }
    }

    // Rule-EDITOR-PANEL-219
    private void redraw() {
        panel.setVisible(!strip.screenshots().isEmpty());
        changed.run();
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    private final class Paste extends DumbAwareAction {
        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }

        // Rule-EDITOR-PANEL-219
        @Override
        public void update(final @NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(onClipboard());
        }

        @Override
        public void actionPerformed(final @NotNull AnActionEvent e) {
            ClipboardContents.withFlavor(DataFlavor.imageFlavor).ifPresent(Screenshots.this::add);
        }
    }
}
