package org.testin.ui.framework;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.components.JBPanel;
import org.intellij.lang.annotations.MagicConstant;
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

    // ------------------------------------------------------------------
    // The declaration — the subclass assigns these in its constructor.
    // ------------------------------------------------------------------

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

    /**
     * Optional: a fixed size for large working dialogs. Setting it also makes
     * the popup resizable and movable.
     */
    protected Dimension preferredSize;

    private DialogDto dto;
    private List<IDialogComponent> built;
    private JBPopup popup;

    protected AbstractFrameworkDialog(final @NotNull Project p) {
        this.p = p;
    }

    private static @NotNull JBPanel<?> verticalStack(final @NotNull List<IDialogComponent> dialogComponents) {
        final JBPanel<?> stack = new JBPanel<>();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        for (final IDialogComponent dialogComponent : dialogComponents) {
            stack.add(dialogComponent.getPanel());
        }
        return stack;
    }

    // ------------------------------------------------------------------
    // What the shell provides.
    // ------------------------------------------------------------------

    private static void installKey(final @NotNull JComponent component,
                                   final @MagicConstant(intValues = {JComponent.WHEN_FOCUSED,
                                           JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                                           JComponent.WHEN_IN_FOCUSED_WINDOW}) int condition,
                                   final @NotNull KeyStroke key, final @NotNull String actionKey, final @NotNull Runnable action) {
        component.getInputMap(condition).put(key, actionKey);
        component.getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                action.run();
            }
        });
    }

    /**
     * The dialog's confirm action; wire it in {@link #shortcuts}.
     */
    protected abstract void submit();

    /**
     * The dialog's primary component, typed: the first declared component
     * that wants the focus (display-only components never qualify).
     */
    @SuppressWarnings("unchecked")
    protected final @NotNull C component() {
        return (C) primaryComponent();
    }

    public final void show() {
        // Assembled on first show: by now the subclass is fully constructed,
        // so its declaration (and its this:: references) is safe to use.
        if (popup == null) {
            final JBPanel<?> contentPanel = buildContentPanel();
            bindShortcutKeys(contentPanel);
            bindSubmitGesture();

            final ComponentPopupBuilder builder = DialogStyle.createPopupBuilder(contentPanel, focusComponent(), dto().title(), null);
            if (preferredSize != null) {
                contentPanel.setPreferredSize(preferredSize);
                builder.setResizable(true).setMovable(true);
            }

            popup = builder.createPopup();
        } else if (popup.isDisposed()) {
            // A JBPopup cannot be reopened after it closes.
            throw new IllegalStateException("This dialog was already shown and closed - create a new instance");
        }

        getPopup().showCenteredInCurrentWindow(p);
    }

    protected final void closeOk() {
        getPopup().closeOk(null);
    }

    // ------------------------------------------------------------------
    // Assembly.
    // ------------------------------------------------------------------

    protected final void closeCancel() {
        getPopup().cancel();
    }

    protected final @NotNull JBPopup getPopup() {
        if (popup == null) {
            throw new IllegalStateException("Dialog popup is created on first show()");
        }
        return popup;
    }

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

    private @NotNull IDialogComponent primaryComponent() {
        for (final IDialogComponent dialogComponent : builtComponents()) {
            if (dialogComponent.wantsFocus()) return dialogComponent;
        }
        return builtComponents().getFirst();
    }

    private @NotNull JComponent focusComponent() {
        return primaryComponent().getFocusComponent();
    }

    /**
     * Content = declared components stacked top to bottom + the status bar.
     * The component claiming {@link IDialogComponent#fillsSpace()} takes the
     * remaining space; the ones above sit on top, the ones below (e.g. a
     * button row) at the bottom. When none claims it, the last one fills.
     */
    private @NotNull JBPanel<?> buildContentPanel() {
        final List<IDialogComponent> all = builtComponents();

        int fillIndex = all.size() - 1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).fillsSpace()) fillIndex = i;
        }

        final JBPanel<?> stack = new JBPanel<>(new BorderLayout());
        stack.setOpaque(false);
        if (fillIndex > 0) {
            stack.add(verticalStack(all.subList(0, fillIndex)), BorderLayout.NORTH);
        }
        stack.add(all.get(fillIndex).getPanel(), BorderLayout.CENTER);
        if (fillIndex < all.size() - 1) {
            stack.add(verticalStack(all.subList(fillIndex + 1, all.size())), BorderLayout.SOUTH);
        }

        final DialogStatusBar statusBar = new DialogStatusBar();
        statusBar.updateItems(dto().shortcuts().toArray(IStatusBarItem[]::new));

        final JBPanel<?> contentPanel = DialogStyle.styleContent(new JBPanel<>(new BorderLayout()));
        contentPanel.setBorder(BorderFactory.createEmptyBorder());
        contentPanel.add(stack, BorderLayout.CENTER);
        contentPanel.add(statusBar.getPanel(), BorderLayout.SOUTH);

        // A popup is not a focus cycle root on its own (a DialogWrapper's root
        // pane was) - without this, Tab wanders instead of cycling through
        // the dialog's fields in layout order.
        contentPanel.setFocusCycleRoot(true);
        contentPanel.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());

        return contentPanel;
    }

    /**
     * Binds every bindable entry's key twice: on each component's focus
     * component (exact pre-multi-component semantics, overriding any inert
     * default binding the field may carry) and on the content panel for
     * whenever the focus is elsewhere inside the dialog.
     */
    private void bindShortcutKeys(final @NotNull JBPanel<?> contentPanel) {
        final List<StatusBarShortcut> declared = dto().shortcuts();
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
            installKey(contentPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, key, actionKey, action);
            for (final IDialogComponent dialogComponent : builtComponents()) {
                if (!dialogComponent.acceptsDialogKeys()) continue;
                installKey(dialogComponent.getFocusComponent(), JComponent.WHEN_FOCUSED, key, actionKey, action);
            }
        }
    }

    /**
     * Every component's own submit gesture (a click on a selection, an OK
     * button) triggers the dialog's submit action.
     */
    private void bindSubmitGesture() {
        for (final IDialogComponent dialogComponent : builtComponents()) {
            dialogComponent.onSubmitRequest(this::submit);
        }
    }
}
