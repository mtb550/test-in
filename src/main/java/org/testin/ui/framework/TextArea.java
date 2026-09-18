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
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.logger.Logger;
import org.testin.util.ClipboardContents;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * A multi-line text area — for pasted content like an error or an exception,
 * and, where the dialog asks for it with {@code images(list)}, screenshots: a
 * pasted image shows as a thumbnail under the box, never as text. Enter inserts
 * a newline (the dialog keys stay off this component), Tab moves the focus like
 * everywhere else, and it claims the dialog's remaining space.
 */
public final class TextArea implements DialogComponent {


    private final @NotNull JBTextArea area;
    private final @NotNull ScreenshotStrip strip;
    private final @NotNull JBPanel<?> panel;

    TextArea(final @NotNull String caption, final @NotNull String placeholder, final @NotNull String value, final int rows, final boolean acceptsImages, final @NotNull List<byte[]> images) {
        area = new JBTextArea(value);
        area.setFont(JBFont.label().biggerOn(2f));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setRows(rows);
        area.setColumns(50);
        area.setBorder(JBUI.Borders.empty(8, 12));

        if (!placeholder.isBlank()) {
            area.getEmptyText().setText(placeholder);
        }

        // Tab traverses the dialog instead of inserting a tab character.
        area.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        area.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        // Bound on the area itself, as the framework's single-line fields
        // are, because a popup or a dialog can eat the keys on the way here.
        // An area that takes images answers paste itself, once, in
        // installImagePaste - so paste is not bound here as well.
        if (acceptsImages) {
            FrameworkTextField.bindAllButPaste(area);
            installImagePaste();
        } else {
            FrameworkTextField.bindClipboard(area);
        }

        final @NotNull JBScrollPane scroll = new JBScrollPane(area);
        scroll.setBorder(JBUI.Borders.emptyTop(8));

        // Under the box, and empty - so no height at all - until a screenshot
        // is stored or pasted (#50).
        strip = new ScreenshotStrip(images);
        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(strip.getPanel(), BorderLayout.SOUTH);

        // UC-EDITOR-PANEL-034, Rule-INTERNAL-087. A caption above the box when
        // the dialog gives one, and the space the box kept above itself goes
        // above the caption instead (#328).
        if (caption.isEmpty()) return;
        final @NotNull JBLabel captionLabel = Caption.of(caption, JBUI.Fonts.label().getSize2D());
        captionLabel.setBorder(JBUI.Borders.empty(8, 0, 2, 0));
        scroll.setBorder(JBUI.Borders.empty());
        panel.add(captionLabel, BorderLayout.NORTH);
    }

    /**
     * Empty when the image cannot be encoded, which the caller reads as "paste it
     * as text instead". It used to signal that by throwing, so a genuine encoding
     * failure and an image the clipboard had not finished loading arrived at the
     * same catch and neither was logged.
     */
    private static byte @NotNull [] toPng(final @NotNull Image image) {
        final BufferedImage buffered;
        if (image instanceof BufferedImage alreadyBuffered) {
            buffered = alreadyBuffered;
        } else {
            final int width = image.getWidth(null);
            final int height = image.getHeight(null);
            // A not-yet-loaded async image reports -1; the caller falls back
            // to the normal text paste.
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

    /**
     * Adds whatever image the clipboard is holding to the strip under the box,
     * and says whether it did. An empty clipboard and text on the clipboard are
     * the same answer - no - which is what makes the caller a single line.
     */
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
            // The clipboard would not hand over the image it just said it had -
            // the normal paste runs instead.
            Logger.warn("Could not read the pasted image: " + ex.getMessage());
            return false;
        }
    }

    /**
     * UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219.
     * <p>
     * The paste gesture, answered once: a picture on the clipboard (e.g. a
     * screenshot) becomes a thumbnail under the box, and anything else is the
     * area's own paste. Copy and cut stay the component's own.
     * <p>
     * A registered action rather than the area's input map, because the IDE
     * dispatches its own paste before a component's input map: a paste bound
     * there never ran. Seen in the sandbox on 2026-09-15 - Ctrl+V pasted
     * nothing, and the log's last action read EditorPaste (#50).
     */
    private void installImagePaste() {
        DumbAwareAction.create(event -> {
            if (!addPastedScreenshot()) area.paste();
        }).registerCustomShortcutSet(CommonShortcuts.getPaste(), area);
    }

    public @NotNull String getText() {
        return area.getText();
    }

    /**
     * The screenshots under the box, in the order they were pasted, and empty
     * for a box that takes none.
     */
    public @NotNull List<byte[]> getImages() {
        return strip.screenshots();
    }

    /**
     * Runs after a screenshot is pasted under the box or taken out of it.
     */
    public void onImagesChanged(final @NotNull Runnable changed) {
        strip.onChange(changed);
    }

    /**
     * Runs after every change to the text, typed or pasted.
     */
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
        // Typing is not a submit gesture; the declared keys save.
    }

    @Override
    public boolean acceptsDialogKeys() {
        // Enter must insert a newline here, never submit the dialog.
        return false;
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }
}
