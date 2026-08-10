package org.testin.ui.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.TextComponentEmptyText;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Unified icon-and-text popup used by create and rename actions.
 */
public abstract class AbstractInputPopupDialog extends AbstractPopupDialog {

    private static final Font FIELD_FONT = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 6f);
    private static final javax.swing.border.Border FIELD_BORDER = JBUI.Borders.empty(10);

    protected final @NotNull ExtendableTextField textField;

    protected AbstractInputPopupDialog(final @NotNull Project project,
                                       final @NotNull String title,
                                       final @Nullable Icon titleIcon,
                                       final @Nullable String placeholder,
                                       final @Nullable String initialValue) {
        super(project, title, titleIcon);

        textField = new ExtendableTextField(initialValue == null ? "" : initialValue);
        textField.setFont(FIELD_FONT);
        textField.setBorder(FIELD_BORDER);

        if (placeholder != null && !placeholder.isBlank()) {
            textField.getEmptyText().setText(placeholder);
            TextComponentEmptyText.setupPlaceholderVisibility(textField);
        }

        addContent(textField, BorderLayout.NORTH);
    }

    protected final void initializeInputPopup() {
        initializePopup(textField);
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                    submitInput();
                    event.consume();
                } else if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    closeCancel();
                    event.consume();
                }
            }
        });
    }

    protected final void setLeadingIcon(final @Nullable Icon icon) {
        DialogStyle.setLeadingIcon(textField, icon);
    }

    protected final void submitInput() {
        final String value = textField.getText().trim();
        if (value.isEmpty()) {
            textField.requestFocusInWindow();
            return;
        }

        onSubmit(value);
        closeOk();
    }

    protected abstract void onSubmit(@NotNull String value);
}
