package org.testin.ui.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Buttonless confirmation popup that follows the same action-dialog design.
 */
public final class ConfirmationPopupDialog extends AbstractPopupDialog {

    private final Runnable confirmAction;

    public ConfirmationPopupDialog(final @NotNull Project project,
                                   final @NotNull String title,
                                   final @Nullable Icon titleIcon,
                                   final @NotNull String message,
                                   final @NotNull Runnable confirmAction) {
        super(project, title, titleIcon);
        this.confirmAction = confirmAction;

        final JBPanel<?> messagePanel = new JBPanel<>(new BorderLayout());
        messagePanel.setOpaque(false);
        messagePanel.setBorder(JBUI.Borders.empty(12));

        final JBLabel messageLabel = new JBLabel("<html>" + message.replace("\n", "<br>") + "</html>");
        messagePanel.add(messageLabel, BorderLayout.CENTER);
        messagePanel.setFocusable(true);
        addContent(messagePanel, BorderLayout.CENTER);

        initializePopup(messagePanel);
        messagePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmAction.run();
                    closeOk();
                    event.consume();
                } else if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    closeCancel();
                    event.consume();
                }
            }
        });
    }
}
