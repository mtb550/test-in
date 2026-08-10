package org.testin.rename;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

/**
 * Rename popup using the same presentation as the create-node popup.
 */
final class RenameDialog {

    private static final Font FIELD_FONT = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 6f);
    private static final Border FIELD_BORDER = JBUI.Borders.empty(10);
    private static final Dimension MIN_SIZE = new Dimension(JBUI.scale(350), 0);

    private final @NotNull Project project;
    private final @NotNull ExtendableTextField textField;
    private final @NotNull JBPopup popup;
    private final @NotNull Consumer<@NotNull String> onSubmit;

    RenameDialog(final @NotNull Project project,
                 final @NotNull String currentName,
                 final @NotNull Consumer<@NotNull String> onSubmit) {
        this.project = project;
        this.onSubmit = onSubmit;

        textField = new ExtendableTextField(currentName);
        textField.setFont(FIELD_FONT);
        textField.setBorder(FIELD_BORDER);

        final JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
        DialogStyle.styleContent(mainPanel);
        mainPanel.setBorder(JBUI.Borders.empty());
        mainPanel.add(textField, BorderLayout.CENTER);

        final Runnable submitAction = this::submit;
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                    submitAction.run();
                    event.consume();
                }
            }
        });

        final ComponentPopupBuilder builder = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, textField)
                .setTitle("Rename")
                .setTitleIcon(new ActiveIcon(AllIcons.Actions.Edit))
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .setResizable(false)
                .setMinSize(MIN_SIZE);

        popup = builder.createPopup();
    }

    void show() {
        popup.showCenteredInCurrentWindow(project);
        textField.selectAll();
        textField.requestFocusInWindow();
    }

    private void submit() {
        onSubmit.accept(textField.getText().trim());
        popup.closeOk(null);
    }
}
