package org.testin.ui.framework;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.statusBar.DialogStatusBar;
import org.testin.statusBar.IStatusBarItem;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * The dialog framework shell (issue #11). A concrete dialog assigns the
 * declaration fields — {@link #title}, {@link #components} and
 * {@link #shortcuts} — in its constructor and implements {@link #submit()}.
 * The shell owns the assembly: components stack top to bottom, the status bar
 * is generated from the same declarations that bind the keys, and the first
 * declared component holds the focus.
 */
public abstract class AbstractFrameworkDialog<C extends IDialogComponent> {

    protected final @NotNull Project p;
    protected String title;
    /**
     * The dialog's content, top to bottom. The first component is the primary
     * one — it holds the focus, carries the key bindings, and its type is the
     * dialog's type parameter (see {@link #component()}).
     */
    protected List<? extends ComponentDialogBase<?>> components;
    /**
     * The status bar mapping — the one declaration that renders the hints and
     * binds the keys. The first bindable entry is the primary action: a
     * component's own submit gesture (e.g. a mouse click on a selection)
     * triggers it.
     */
    protected List<StatusBarShortcut> shortcuts;
    private DialogDto dto;

    // ------------------------------------------------------------------
    // The declaration — the subclass assigns these in its constructor.
    // ------------------------------------------------------------------
    private List<IDialogComponent> built;
    private JBPopup popup;

    protected AbstractFrameworkDialog(final @NotNull Project p) {
        this.p = p;
    }

    /**
     * The dialog's confirm action; wire it in {@link #shortcuts}.
     */
    protected abstract void submit();

    // ------------------------------------------------------------------
    // What the shell provides.
    // ------------------------------------------------------------------

    /**
     * The first declared component, typed.
     */
    @SuppressWarnings("unchecked")
    protected final @NotNull C component() {
        return (C) builtComponents().getFirst();
    }

    public final void show() {
        // Assembled on first show: by now the subclass is fully constructed,
        // so its declaration (and its this:: references) is safe to use.
        if (popup == null) {
            final JBPanel<?> contentPanel = buildContentPanel();
            bindShortcutKeys();
            bindSubmitGesture();
            popup = DialogStyle.createPopupBuilder(contentPanel, focusComponent(), dto().title(), null).createPopup();
        } else if (popup.isDisposed()) {
            // A JBPopup cannot be reopened after it closes.
            throw new IllegalStateException("This dialog was already shown and closed - create a new instance");
        }

        getPopup().showCenteredInCurrentWindow(p);
    }

    protected final void closeOk() {
        getPopup().closeOk(null);
    }

    protected final void closeCancel() {
        getPopup().cancel();
    }

    protected final @NotNull JBPopup getPopup() {
        if (popup == null) {
            throw new IllegalStateException("Dialog popup is created on first show()");
        }
        return popup;
    }

    // ------------------------------------------------------------------
    // Assembly.
    // ------------------------------------------------------------------

    /**
     * Packages the declared fields exactly once; @NonNull reports a forgotten part.
     */
    private @NotNull DialogDto dto() {
        if (dto == null) {
            dto = DialogDto.builder()
                    .title(title)
                    .components(components)
                    .shortcuts(shortcuts)
                    .build();
        }
        return dto;
    }

    /**
     * The declared components, built exactly once — they hold Swing state.
     */
    private @NotNull List<IDialogComponent> builtComponents() {
        if (built == null) {
            built = new ArrayList<>();
            for (final ComponentDialogBase<?> holder : dto().components()) {
                built.add(holder.getComponent());
            }
            if (built.isEmpty()) {
                throw new IllegalStateException("A dialog needs at least one component");
            }
        }
        return built;
    }

    private @NotNull JComponent focusComponent() {
        return builtComponents().getFirst().getFocusComponent();
    }

    /**
     * Content = declared components stacked top to bottom + the status bar.
     */
    private @NotNull JBPanel<?> buildContentPanel() {
        final JBPanel<?> stack = new JBPanel<>();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        for (final IDialogComponent dialogComponent : builtComponents()) {
            stack.add(dialogComponent.getPanel());
        }

        final DialogStatusBar statusBar = new DialogStatusBar();
        statusBar.updateItems(dto().shortcuts().toArray(IStatusBarItem[]::new));

        final JBPanel<?> contentPanel = DialogStyle.styleContent(new JBPanel<>(new BorderLayout()));
        contentPanel.setBorder(BorderFactory.createEmptyBorder());
        contentPanel.add(stack, BorderLayout.CENTER);
        contentPanel.add(statusBar.getPanel(), BorderLayout.SOUTH);
        return contentPanel;
    }

    /**
     * Binds every bindable entry's key on the focus component.
     */
    private void bindShortcutKeys() {
        final List<StatusBarShortcut> declared = dto().shortcuts();
        final JComponent focusComponent = focusComponent();
        final Set<KeyStroke> bound = new HashSet<>();

        for (int i = 0; i < declared.size(); i++) {
            final StatusBarShortcut shortcut = declared.get(i);
            if (!shortcut.isBindable()) continue;

            // isBindable guarantees both; requireNonNull makes that visible to dataflow.
            final KeyStroke key = Objects.requireNonNull(shortcut.shortcut()).getKey();
            final Runnable action = Objects.requireNonNull(shortcut.action());

            // Two entries on one key would silently shadow each other.
            if (!bound.add(key)) {
                throw new IllegalStateException("Duplicate dialog shortcut: " + shortcut.getShortcutText());
            }

            final String actionKey = "testin.framework.shortcut." + i;
            focusComponent.getInputMap(JComponent.WHEN_FOCUSED).put(key, actionKey);
            focusComponent.getActionMap().put(actionKey, new AbstractAction() {
                @Override
                public void actionPerformed(final ActionEvent event) {
                    action.run();
                }
            });
        }
    }

    /**
     * Every component's own submit gesture triggers the primary action.
     */
    private void bindSubmitGesture() {
        dto().shortcuts().stream()
                .filter(StatusBarShortcut::isBindable)
                .findFirst()
                .ifPresent(primary -> builtComponents()
                        .forEach(dialogComponent -> dialogComponent.onSubmitRequest(Objects.requireNonNull(primary.action()))));
    }
}
