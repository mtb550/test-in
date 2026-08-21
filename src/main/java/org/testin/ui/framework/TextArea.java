package org.testin.ui.framework;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import org.testin.util.ClipboardContents;
import org.testin.logger.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * A multi-line text area — for pasted content like an error, an exception, or
 * a screenshot (pasted images become a base64 PNG data-URI). Enter inserts a
 * newline (the dialog keys stay off this component), Tab moves the focus like
 * everywhere else, and it claims the dialog's remaining space.
 */
public final class TextArea implements DialogComponent {

    /**
     * The paste that inserts nothing, standing in for a look and feel that
     * supplies no paste action of its own.
     */
    private static final @NotNull Action NO_PASTE = new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent event) {
        }
    };


    private final @NotNull JBTextArea area;
    private final @NotNull JBScrollPane panel;

    TextArea(final @NotNull String placeholder, final @NotNull String value, final int rows) {
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

        installImagePaste();

        panel = new JBScrollPane(area);
        panel.setBorder(JBUI.Borders.emptyTop(8));
    }

    /**
     * Empty when the image cannot be encoded, which the caller reads as "paste it
     * as text instead". It used to signal that by throwing, so a genuine encoding
     * failure and an image the clipboard had not finished loading arrived at the
     * same catch and neither was logged.
     */
    private static @NotNull String toDataUri(final @NotNull Image image) {
        final BufferedImage buffered;
        if (image instanceof BufferedImage alreadyBuffered) {
            buffered = alreadyBuffered;
        } else {
            final int width = image.getWidth(null);
            final int height = image.getHeight(null);
            // A not-yet-loaded async image reports -1; the caller falls back
            // to the normal text paste.
            if (width <= 0 || height <= 0) return "";

            buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            final Graphics2D g = buffered.createGraphics();
            try {
                g.drawImage(image, 0, 0, null);
            } finally {
                g.dispose();
            }
        }

        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(buffered, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (final IOException ex) {
            Logger.error("Could not encode a pasted image as PNG: " + ex.getMessage());
            return "";
        }
    }

    /**
     * Inserts whatever image the clipboard is holding as a data URI, and says
     * whether it did. An empty clipboard and text on the clipboard are the same
     * answer - no - which is what makes the caller a single line.
     */
    private boolean insertPastedImage() {
        return ClipboardContents.withFlavor(DataFlavor.imageFlavor)
                .map(this::insertAsDataUri)
                .orElse(false);
    }

    private boolean insertAsDataUri(final @NotNull Transferable contents) {
        try {
            final String dataUri = toDataUri((Image) contents.getTransferData(DataFlavor.imageFlavor));
            if (dataUri.isEmpty()) return false;

            area.insert(dataUri, area.getCaretPosition());
            return true;
        } catch (final UnsupportedFlavorException | IOException ex) {
            // The clipboard would not hand over the image it just said it had -
            // the normal paste runs instead.
            Logger.warn("Could not read the pasted image: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Ctrl+V with an image on the clipboard (e.g. a screenshot) inserts it as
     * a base64 PNG data-URI; plain text pastes as always. Copy and cut stay
     * the component's own.
     */
    private void installImagePaste() {
        // A text area always has a paste action; one whose look and feel somehow
        // does not gets an action that inserts nothing, so the fallback below is
        // unconditional.
        final Action defaultPaste = Objects.requireNonNullElse(
                area.getActionMap().get(DefaultEditorKit.pasteAction), NO_PASTE);
        area.getActionMap().put(DefaultEditorKit.pasteAction, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                if (insertPastedImage()) return;
                defaultPaste.actionPerformed(event);
            }
        });
    }

    public @NotNull String getText() {
        return area.getText();
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
