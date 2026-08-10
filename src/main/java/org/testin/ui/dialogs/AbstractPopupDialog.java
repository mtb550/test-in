package org.testin.ui.dialogs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Common popup shell for project actions that need the unified dialog design.
 */
public abstract class AbstractPopupDialog {

    protected final @NotNull Project project;
    protected final @NotNull JBPanel<?> contentPanel;

    private final @NotNull String title;
    private final @Nullable Icon titleIcon;
    private JBPopup popup;

    protected AbstractPopupDialog(final @NotNull Project project,
                                  final @NotNull String title,
                                  final @Nullable Icon titleIcon) {
        this.project = project;
        this.title = title;
        this.titleIcon = titleIcon;
        this.contentPanel = DialogStyle.styleContent(new JBPanel<>(new BorderLayout()));
        this.contentPanel.setBorder(BorderFactory.createEmptyBorder());
    }

    protected final void addContent(final @NotNull Component component, final Object constraint) {
        contentPanel.add(component, constraint);
    }

    protected final void initializePopup(final @NotNull JComponent focusComponent) {
        if (popup != null) {
            throw new IllegalStateException("Dialog popup is already initialized");
        }

        popup = DialogStyle.createPopupBuilder(contentPanel, focusComponent, title, titleIcon).createPopup();
    }

    protected final @NotNull JBPopup getPopup() {
        if (popup == null) {
            throw new IllegalStateException("Dialog popup has not been initialized");
        }
        return popup;
    }

    protected final void closeOk() {
        getPopup().closeOk(null);
    }

    protected final void closeCancel() {
        getPopup().cancel();
    }

    public final void show() {
        getPopup().showCenteredInCurrentWindow(project);
    }
}
