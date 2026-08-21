package org.testin.ui.framework;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.components.JBPanel;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.testin.statusbar.DialogStatusBar;
import org.testin.statusbar.StatusBarItem;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.Optional;

/**
 * The dialog framework shell (issue #11). A concrete dialog assigns the
 * declaration fields — {@link #title}, {@link #components} and
 * {@link #shortcuts} — in its constructor and implements {@link #submit()}.
 * The shell owns the assembly: components stack top to bottom, the status bar
 * is generated from the same declarations that bind the keys, and the first
 * declared component holds the focus.
 */
public abstract class AbstractFrameworkDialog<C extends DialogComponent> {

    protected final @NotNull Project p;

    // ------------------------------------------------------------------
    // The declaration — the subclass assigns these in its constructor.
    // ------------------------------------------------------------------

    protected @NotNull String title ;
    /**
     * The dialog's content, top to bottom. The first component is the primary
     * one — it holds the focus, carries the key bindings, and its type is the
     * dialog's type parameter (see {@link #component()}).
     */
    protected @NotNull List<? extends ComponentDialogBase<?>> components ;
    /**
     * The status bar mapping — the one declaration that renders the hints and
     * binds the keys. The first bindable entry is the primary action: a
     * component's own submit gesture (e.g. a mouse click on a selection)
     * triggers it.
     */
    protected @NotNull List<StatusBarShortcut> shortcuts ;

    /**
     * Optional: a fixed size for large working dialogs. Setting it also makes
     * the popup resizable and movable. Zero, the default, is a dialog that
     * sizes itself to its content - which is what most of them do.
     */
    protected @NotNull Dimension preferredSize = new Dimension();

    /**
     * Built on first show and kept: the declaration, the components that hold
     * the Swing state, and the popup itself. Empty until then, because a
     * subclass has not finished declaring itself while its constructor runs.
     */
    private @NotNull Optional<DialogDto> dto = Optional.empty();
    private @NotNull Optional<List<DialogComponent>> built = Optional.empty();
    private @NotNull Optional<JBPopup> popup = Optional.empty();

    protected AbstractFrameworkDialog(final @NotNull Project p) {
        this.p = p;
    }

    private static @NotNull JBPanel<?> verticalStack(final @NotNull List<DialogComponent> dialogComponents) {
        final @NotNull JBPanel<?> stack = new JBPanel<>();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        for (final DialogComponent dialogComponent : dialogComponents) {
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
     * What the dialog does when it is confirmed; wire it in {@link #shortcuts}.
     */
    protected abstract void submit();

    /**
     * The dialog's primary component, typed: the first declared component
     * that wants the focus (display-only components never qualify).
     */
    // Unchecked by necessity and safe by construction: a dialog names its own
    // type parameter and declares its own components, so the first one that
    // wants the focus is the C it said it was.
    @SuppressWarnings("unchecked")
    protected final @NotNull C component() {
        return (C) primaryComponent();
    }

    public final void show() {
        // Assembled on first show: by now the subclass is fully constructed,
        // so its declaration (and its this:: references) is safe to use.
        if (popup.isEmpty()) {
            popup = Optional.of(buildPopup());
        } else if (getPopup().isDisposed()) {
            // A JBPopup cannot be reopened after it closes.
            throw new IllegalStateException("This dialog was already shown and closed - create a new instance");
        }

        getPopup().showCenteredInCurrentWindow(p);
    }

    private @NotNull JBPopup buildPopup() {
        final @NotNull JBPanel<?> contentPanel = buildContentPanel();
        bindShortcutKeys(contentPanel);
        bindSubmitGesture();

        final @NotNull ComponentPopupBuilder builder = DialogStyle.createPopupBuilder(contentPanel, focusComponent(), dto().title());
        if (preferredSize.width > 0) {
            contentPanel.setPreferredSize(preferredSize);
            builder.setResizable(true).setMovable(true);
        }

        return builder.createPopup();
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
        return popup.orElseThrow(() -> new IllegalStateException("Dialog popup is created on first show()"));
    }

    /**
     * Packages the declared fields exactly once; @NonNull reports a forgotten part.
     */
    private @NotNull DialogDto dto() {
        if (dto.isEmpty()) {
            dto = Optional.of(DialogDto.builder()
                    .title(title)
                    .components(components)
                    .shortcuts(shortcuts)
                    .build());
        }
        return dto.orElseThrow();
    }

    /**
     * The declared components, built exactly once — they hold Swing state.
     */
    private @NotNull List<DialogComponent> builtComponents() {
        if (built.isEmpty()) {
            final @NotNull List<DialogComponent> dialogComponents = new ArrayList<>();
            for (final ComponentDialogBase<?> holder : dto().components()) {
                dialogComponents.add(holder.getComponent());
            }
            if (dialogComponents.isEmpty()) {
                throw new IllegalStateException("A dialog needs at least one component");
            }

            built = Optional.of(dialogComponents);
        }
        return built.orElseThrow();
    }

    private @NotNull DialogComponent primaryComponent() {
        for (final DialogComponent dialogComponent : builtComponents()) {
            if (dialogComponent.wantsFocus()) return dialogComponent;
        }
        return builtComponents().getFirst();
    }

    private @NotNull JComponent focusComponent() {
        return primaryComponent().getFocusComponent();
    }

    /**
     * Content = declared components stacked top to bottom + the status bar.
     * The component claiming {@link DialogComponent#fillsSpace()} takes the
     * remaining space; the ones above sit on top, the ones below (e.g. a
     * button row) at the bottom. When none claims it, the last one fills.
     */
    private @NotNull JBPanel<?> buildContentPanel() {
        final @NotNull List<DialogComponent> all = builtComponents();

        int fillIndex = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).fillsSpace()) fillIndex = i;
        }

        // Nothing asked for the space, so the last component takes it - but not a
        // component that must keep its height. Handing it to a button row put the
        // button in the middle of the dialog instead of at the bottom.
        for (int i = all.size() - 1; i >= 0 && fillIndex < 0; i--) {
            if (all.get(i).canFillSpace()) fillIndex = i;
        }

        // Every component refused it; something has to go in the center.
        if (fillIndex < 0) fillIndex = all.size() - 1;

        final @NotNull JBPanel<?> stack = new JBPanel<>(new BorderLayout());
        stack.setOpaque(false);
        if (fillIndex > 0) {
            stack.add(verticalStack(all.subList(0, fillIndex)), BorderLayout.NORTH);
        }
        stack.add(all.get(fillIndex).getPanel(), BorderLayout.CENTER);
        if (fillIndex < all.size() - 1) {
            stack.add(verticalStack(all.subList(fillIndex + 1, all.size())), BorderLayout.SOUTH);
        }

        final @NotNull DialogStatusBar statusBar = new DialogStatusBar();
        statusBar.updateItems(dto().shortcuts().toArray(StatusBarItem[]::new));

        final @NotNull JBPanel<?> contentPanel = DialogStyle.styleContent(new JBPanel<>(new BorderLayout()));
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
        final @NotNull List<StatusBarShortcut> declared = dto().shortcuts();
        final @NotNull Set<KeyStroke> bound = new HashSet<>();

        for (int i = 0; i < declared.size(); i++) {
            final @NotNull StatusBarShortcut shortcut = declared.get(i);
            if (!shortcut.isBindable()) continue;

            // isBindable guarantees both; requireNonNull makes that visible to dataflow.
            final @NotNull KeyStroke key = Objects.requireNonNull(shortcut.shortcut()).getKey();
            final @NotNull Runnable action = Objects.requireNonNull(shortcut.action());

            // Two entries on one key would silently shadow each other.
            if (!bound.add(key)) {
                throw new IllegalStateException("Duplicate dialog shortcut: " + shortcut.getShortcutText());
            }

            final @NotNull String actionKey = "testin.framework.shortcut." + i;
            installKey(contentPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, key, actionKey, action);
            for (final DialogComponent dialogComponent : builtComponents()) {
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
        for (final DialogComponent dialogComponent : builtComponents()) {
            dialogComponent.onSubmitRequest(this::submit);
        }
    }
}
