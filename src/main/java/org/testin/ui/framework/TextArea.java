package org.testin.ui.framework;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * A multi-line text area — for pasted content like an error, an exception, or
 * a screenshot (pasted images become a base64 PNG data-URI). Enter inserts a
 * newline (the dialog keys stay off this component), Tab moves the focus like
 * everywhere else, and it claims the dialog's remaining space.
 */
public final class TextArea implements IDialogComponent {

    private final @NotNull JBTextArea area;
    private final @NotNull JBScrollPane panel;

    TextArea(final @Nullable String placeholder, final @Nullable String value, final int rows) {
        area = new JBTextArea(value == null ? "" : value);
        area.setFont(JBFont.label().biggerOn(2f));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setRows(rows);
        area.setColumns(50);
        area.setBorder(JBUI.Borders.empty(8, 12));

        if (placeholder != null && !placeholder.isBlank()) {
            area.getEmptyText().setText(placeholder);
        }

        // Tab traverses the dialog instead of inserting a tab character.
        area.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        area.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        installImagePaste();

        panel = new JBScrollPane(area);
        panel.setBorder(JBUI.Borders.emptyTop(8));
    }

    private static @NotNull String toDataUri(final @NotNull Image image) throws Exception {
        final BufferedImage buffered;
        if (image instanceof BufferedImage alreadyBuffered) {
            buffered = alreadyBuffered;
        } else {
            final int width = image.getWidth(null);
            final int height = image.getHeight(null);
            // A not-yet-loaded async image reports -1; the caller falls back
            // to the normal text paste.
            if (width <= 0 || height <= 0) throw new IllegalStateException("image not loaded");

            buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            final Graphics2D g = buffered.createGraphics();
            try {
                g.drawImage(image, 0, 0, null);
            } finally {
                g.dispose();
            }
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(buffered, "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /**
     * Ctrl+V with an image on the clipboard (e.g. a screenshot) inserts it as
     * a base64 PNG data-URI; plain text pastes as always. Copy and cut stay
     * the component's own.
     */
    private void installImagePaste() {
        final Action defaultPaste = area.getActionMap().get(DefaultEditorKit.pasteAction);
        area.getActionMap().put(DefaultEditorKit.pasteAction, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                final Transferable contents = CopyPasteManager.getInstance().getContents();
                if (contents != null && contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    try {
                        final Image image = (Image) contents.getTransferData(DataFlavor.imageFlavor);
                        area.insert(toDataUri(image), area.getCaretPosition());
                        return;
                    } catch (final Exception ignored) {
                        // Unreadable image - fall through to the normal paste.
                    }
                }
                if (defaultPaste != null) defaultPaste.actionPerformed(event);
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
