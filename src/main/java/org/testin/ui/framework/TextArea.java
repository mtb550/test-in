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

import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.ui.Caption;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Fonts;
import org.testin.util.ClipboardContents;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public final class TextArea implements DialogComponent {
    private final @NotNull JBTextArea area;
    private final @NotNull ScreenshotStrip strip;
    private final @NotNull JBPanel<?> panel;

    TextArea(final @NotNull String caption, final @NotNull String placeholder, final @NotNull String value, final int rows, final boolean acceptsImages, final @NotNull List<byte[]> images) {
        area = new JBTextArea(value);
        area.setFont(Fonts.field());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setRows(rows);
        area.setColumns(50);
        area.setBorder(JBUI.Borders.empty(8, 12));

        if (!placeholder.isBlank()) {
            area.getEmptyText().setText(placeholder);
            area.getEmptyText().setFont(Fonts.placeholder());
        }

        area.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        area.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        if (acceptsImages) {
            FrameworkTextField.bindAllButPaste(area);
            installImagePaste();
        } else {
            FrameworkTextField.bindClipboard(area);
        }

        final @NotNull JBScrollPane scroll = new JBScrollPane(area);
        DialogStyle.framed(scroll);
        scroll.setViewportBorder(JBUI.Borders.empty());

        strip = new ScreenshotStrip(images);

        // UC-EDITOR-PANEL-034, Rule-INTERNAL-087
        panel = Caption.above(caption, JBUI.Panels.simplePanel(scroll).addToBottom(strip.getPanel()).andTransparent());
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

    private boolean addPastedScreenshot() {
        return ClipboardContents.withFlavor(DataFlavor.imageFlavor)
                .map(this::addScreenshot)
                .orElse(false);
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    private boolean addScreenshot(final @NotNull Transferable contents) {
        try {
            final byte @NotNull [] png = toPng((Image) contents.getTransferData(DataFlavor.imageFlavor));
            if (png.length == 0) return false;

            strip.add(png);
            return true;
        } catch (final UnsupportedFlavorException | IOException ex) {
            Logger.warn("Could not read the pasted image: " + ex.getMessage());
            return false;
        }
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219
    private void installImagePaste() {
        DumbAwareAction.create(_ -> {
            if (!addPastedScreenshot()) area.paste();
        }).registerCustomShortcutSet(CommonShortcuts.getPaste(), area);
    }

    public @NotNull String getText() {
        return area.getText();
    }

    public @NotNull List<byte[]> getImages() {
        return strip.screenshots();
    }

    public void onImagesChanged(final @NotNull Runnable changed) {
        strip.onChange(changed);
    }

    public void onTextChanged(final @NotNull Runnable changed) {
        area.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final @NotNull DocumentEvent event) {
                changed.run();
            }
        });
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return area;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    @Override
    public boolean acceptsDialogKeys() {
        return false;
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }
}
