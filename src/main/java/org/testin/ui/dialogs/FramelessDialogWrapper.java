package org.testin.ui.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Shared presentation for project-owned dialogs.
 * <p>
 * The dialog remains a DialogWrapper so callers keep the normal modal,
 * validation, exit-code, and showAndGet behaviour. Only its chrome is
 * replaced with the plugin's lightweight, buttonless presentation.
 */
public abstract class FramelessDialogWrapper extends DialogWrapper {

    protected FramelessDialogWrapper(final @Nullable Project project, final boolean canBeParent) {
        super(project, canBeParent);
        setUndecorated(true);
    }

    protected FramelessDialogWrapper(final boolean canBeParent) {
        super(canBeParent);
        setUndecorated(true);
    }

    /**
     * Call after the title and dialog fields have been initialized.
     */
    protected final void initFrameless() {
        init();
        installKeyboardActions();
    }

    @Override
    protected final Action @NotNull [] createActions() {
        return new Action[0];
    }

    /**
     * No south panel at all: the frameless presentation has no button row.
     */
    @Override
    protected final @Nullable JComponent createSouthPanel() {
        return null;
    }

    @Override
    protected @NotNull JComponent createNorthPanel() {
        final JBPanel<?> header = new JBPanel<>(new BorderLayout());
        header.setOpaque(true);
        header.setBackground(JBUI.CurrentTheme.Popup.headerBackground(true));
        header.setBorder(JBUI.Borders.empty(12, 16, 4, 16));

        final JBLabel title = new JBLabel(getTitle());
        title.setForeground(JBUI.CurrentTheme.Popup.headerForeground(true));
        title.setFont(JBFont.label().deriveFont(Font.BOLD, JBUI.Fonts.label().getSize2D() + 2f));
        header.add(title, BorderLayout.WEST);
        return header;
    }

    @Override
    protected javax.swing.border.@NotNull Border createContentPaneBorder() {
        return JBUI.Borders.empty(4, 16, 16, 16);
    }

    private void installKeyboardActions() {
        final JRootPane rootPane = getRootPane();
        final InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "testin.dialog.ok");
        actionMap.put("testin.dialog.ok", new AbstractAction() {
            @Override
            public void actionPerformed(final @NotNull ActionEvent event) {
                doOKAction();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "testin.dialog.cancel");
        actionMap.put("testin.dialog.cancel", new AbstractAction() {
            @Override
            public void actionPerformed(final @NotNull ActionEvent event) {
                doCancelAction();
            }
        });
    }
}
