/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.ui.framework;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.ui.components.JBPanel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.model.StatusBarItem;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Function;

/**
 * The dialog framework shell (issue #11). A concrete dialog assigns the
 * declaration fields — {@link #title}, {@link #components} and
 * {@link #shortcuts} — in its constructor and implements {@link #submit()}.
 * The shell owns the assembly: components stack top to bottom, the status bar
 * is generated from the same declarations that bind the keys, and the first
 * declared component holds the focus.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractFrameworkDialog<C extends DialogComponent> {

    protected final @NotNull Project p;

    // ------------------------------------------------------------------
    // The declaration — the subclass assigns these in its constructor.
    // ------------------------------------------------------------------

    protected @NotNull String title = "";
    /**
     * The dialog's content, top to bottom. The first component is the primary
     * one — it holds the focus, carries the key bindings, and its type is the
     * dialog's type parameter (see {@link #component()}).
     */
    protected @NotNull List<? extends ComponentDialogBase<?>> components = List.of();
    /**
     * The status bar mapping — the one declaration that renders the hints and
     * binds the keys. The first bindable entry is the primary action: a
     * component's own submit gesture (e.g. a mouse click on a selection)
     * triggers it.
     */
    protected @NotNull List<StatusBarShortcut> shortcuts = List.of();

    /**
     * Optional: a fixed size for large working dialogs. Setting it also makes
     * the popup resizable and movable. Zero, the default, is a dialog that
     * sizes itself to its content - which is what most of them do.
     */
    protected @NotNull Dimension preferredSize = new Dimension();

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-076.
     * <p>
     * Whether clicking away closes this dialog.
     * <p>
     * <b>False by default, and that is the important half.</b> A dialog holding
     * something the tester typed - a name, a commit message, a bulk edit - must
     * not lose it to a stray click on the editor behind it; Escape cancels.
     * <p>
     * True for a dialog that holds nothing and is only asked a question: the
     * search is looked at and left, and a tester who clicks somewhere else has
     * finished with it. Left open it sits behind whatever they clicked, and the
     * next press of its shortcut looks to them like it did nothing (#66,
     * finding 104).
     */
    protected boolean dismissOnClickOutside;

    /**
     * Whether the tester can drag this dialog's edges and move it. A dialog that
     * sets {@link #preferredSize} is resizable already; this is for one that
     * sizes itself to its content and can still be made wider - the test case
     * form, whose fields the tester may want longer than it opened.
     */
    protected boolean resizable;

    /**
     * Built on first show and kept: the declaration, the components that hold
     * the Swing state, and the popup itself. Empty until then, because a
     * subclass has not finished declaring itself while its constructor runs.
     */
    private @NotNull Optional<DialogDto> dto = Optional.empty();
    private @NotNull Optional<List<DialogComponent>> built = Optional.empty();
    private @NotNull Optional<JBPopup> popup = Optional.empty();

    /**
     * The strip along the bottom, once the popup is built, and what it was last
     * asked to show - kept for the strip that does not exist yet.
     */
    private @NotNull Optional<StatusBarBase> strip = Optional.empty();
    private @NotNull Optional<StatusBarItem[]> keysShown = Optional.empty();


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

    /**
     * What the dialog does when it is confirmed; wire it in {@link #shortcuts}.
     */
    protected abstract void submit();

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-067.
     * <p>
     * What the tester typed into that field, once it is something this dialog
     * will take - and empty when it is not, with the field already marked as the
     * one holding the dialog open.
     * <p>
     * Six dialogs opened {@code submit()} with the same four lines: read the
     * field, trim it, warn if it is empty, return. They agreed only because
     * nobody had yet changed one of them, and the one that did not agree was the
     * report dialog, which moved the cursor and said nothing (#251, #11).
     * <p>
     * Trimmed here rather than by the field, because a surrounding space is a
     * question about the value and not about the component holding it.
     */
    protected final @NotNull String accepted(final @NotNull TextValue field) {
        final @NotNull String value = field.getText().trim();
        if (value.isEmpty()) field.showEmptyWarning();

        return value;
    }

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-067.
     * <p>
     * The same, for a field that also has a rule about what it may hold - a
     * repository address, an email address, a name that can become a Java
     * package.
     * <p>
     * <b>Empty means the dialog will not take what is there</b>, whether the
     * field was blank or the value was refused, so a caller has one thing to
     * test and the same thing to do about it. Which of the two it was has
     * already been said, in the place that says it best: the field itself when
     * nothing was typed, and a sentence naming the value when something was.
     * <p>
     * Refused here rather than after the dialog closes, which is the whole point
     * of asking in a dialog of ours - the tester is still looking at what they
     * typed and can correct it.
     */
    protected final @NotNull String accepted(final @NotNull TextValue field, final @NotNull Predicate<String> allows, final @NotNull Refused refusal) {
        return accepted(field, value -> allows.test(value) ? Optional.empty() : Optional.of(refusal));
    }

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-067.
     * <p>
     * The same, for a field whose rule can be broken in more than one way and
     * says which.
     * <p>
     * A node name is the first of those: it has to be one folder inside the one
     * selected, and for two kinds of node it also has to be a name Java can take
     * as a package. The two read differently and a tester needs to be told which
     * one they met, so the value is asked what is wrong with it rather than
     * whether anything is (#312, A65).
     */
    protected final @NotNull String accepted(final @NotNull TextValue field, final @NotNull Function<String, Optional<Refused>> refusing) {
        final @NotNull String value = accepted(field);
        if (value.isEmpty()) return value;

        final @NotNull Optional<Refused> refusal = refusing.apply(value);
        if (refusal.isEmpty()) return value;

        Services.getInstance(p, Notifier.class).softRefuse(p, refusal.orElseThrow(), value);
        return "";
    }

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-057.
     * <p>
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

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-061, Rule-INTERNAL-075.
     * <p>
     * Puts this dialog on screen - unless one of its kind is already there, in
     * which case that one is brought forward and this instance is dropped.
     * <p>
     * <b>One of a kind at a time.</b> These are popups rather than modal
     * windows, and none of them closes on losing the focus: a click outside
     * never dismisses a Testin dialog, Escape does. So anything that can open a
     * dialog twice did - the global search is in the keymap, so its shortcut
     * fires from inside the search dialog already open, and two ended up
     * stacked (#66, finding 104).
     * <p>
     * Here rather than in the openers, because there is one rule and twenty-odd
     * of them, and the next one cannot forget it.
     * <p>
     * <b>Raised or replaced.</b> A dialog holding what the tester typed is raised,
     * so nothing typed is lost. One holding nothing of theirs - a confirmation, a
     * node's details, a screenshot - is replaced by the newer one: raised, a
     * standing confirmation swallowed every later one, and Delete pressed on a
     * second node brought the first question forward, where Enter removed the
     * first node (#66, finding 168).
     *
     * @return whether this instance went on screen. False when an older one of
     * its kind was raised instead, and this one never built its popup.
     */
    public final boolean show() {
        final @NotNull OpenDialogs open = Services.getInstance(p, OpenDialogs.class);

        final @NotNull Optional<JBPopup> already = open.shown(getClass());
        if (already.isPresent()) {
            if (!replacesItsKind()) {
                already.orElseThrow().getContent().requestFocusInWindow();
                return false;
            }

            // Closed unanswered, which is what Escape does: a question about an
            // older gesture cannot then be confirmed by the key meant for this one.
            already.orElseThrow().cancel();
        }

        // Assembled on first show: by now the subclass is fully constructed,
        // so its declaration (and its this:: references) is safe to use.
        if (popup.isEmpty()) {
            popup = Optional.of(buildPopup());
        } else if (getPopup().isDisposed()) {
            // A JBPopup cannot be reopened after it closes.
            throw new IllegalStateException("This dialog was already shown and closed - create a new instance");
        }

        open.remember(getClass(), getPopup());
        getPopup().showCenteredInCurrentWindow(p);
        return true;
    }

    /**
     * Runs when the dialog closes, whichever way it closed - submitted,
     * canceled, or replaced by another of its kind. A popup, unlike the modal it
     * replaced, does not return an answer to the line that showed it, so this is
     * how a caller waits for it: a cell editor that has to stop editing either
     * way, and a question that must not open over the one before it.
     * <p>
     * Asked of a dialog that was shown; {@link #show} answers whether it was.
     */
    public final void onClosed(final @NotNull Runnable action) {
        getPopup().addListener(new JBPopupListener() {
            @Override
            public void onClosed(final @NotNull LightweightWindowEvent event) {
                action.run();
            }
        });
    }

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-075.
     * <p>
     * Whether a newer dialog of this kind replaces one already on screen rather
     * than raising it. False for anything the tester types into; true for a
     * dialog that only shows something or asks one question, where the newer
     * request is the one they meant.
     */
    protected boolean replacesItsKind() {
        return false;
    }

    private @NotNull JBPopup buildPopup() {
        final @NotNull JBPanel<?> contentPanel = buildContentPanel();
        bindShortcutKeys(contentPanel);
        bindSubmitGesture();

        final @NotNull ComponentPopupBuilder builder = DialogStyle.createPopupBuilder(contentPanel, focusComponent(), dto().title(), dismissOnClickOutside);
        if (preferredSize.width > 0) {
            contentPanel.setPreferredSize(preferredSize);
            builder.setResizable(true).setMovable(true);
        }
        if (resizable) builder.setResizable(true).setMovable(true);

        builder.addListener(new JBPopupListener() {
            @Override
            public void onClosed(final @NotNull LightweightWindowEvent event) {
                closed();
            }
        });

        return builder.createPopup();
    }

    /**
     * Runs when the dialog closes, whichever way. For a dialog holding something
     * that has to be released - a listener added to the whole application, say -
     * which a caller-side {@link #onClosed} cannot do, because the dialog is the
     * one that knows it holds it. Nothing by default.
     */
    protected void closed() {
    }

    /**
     * UC-INTERNAL-007.
     * <p>
     * What the strip along the bottom shows, for a dialog whose keys change with
     * where the cursor is - the test case form shows each field's own. Only
     * what is shown: the keys stay bound by whoever binds them.
     */
    protected final void showKeys(final StatusBarItem @NotNull [] items) {
        keysShown = Optional.of(items);
        strip.ifPresent(bar -> bar.updateItems(items));
    }

    /**
     * Sizes the dialog to its content again, after the content grew or shrank,
     * and scrolls whatever holds the keyboard back into view.
     * <p>
     * The height is set rather than packed. {@code pack()} asks the popup to
     * work its size out again and a resizable one will not - the test case form
     * grew by nothing while that ran, and dragging it taller showed the lines
     * added several keystrokes before. The width is left alone, including one
     * the tester chose.
     * <p>
     * Nothing before the popup exists: content can grow while the dialog is
     * still being built.
     */
    public final void refit() {
        popup.ifPresent(open -> {
            open.setSize(new Dimension(open.getSize().width, naturalHeightOf(open.getContent())));

            ApplicationManager.getApplication().invokeLater(() -> {
                final @NotNull Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof JComponent jComp) {
                    jComp.scrollRectToVisible(new Rectangle(0, 0, jComp.getWidth(), jComp.getHeight()));
                }
            });
        });
    }

    /**
     * What a component would ask for if nobody had told it a size.
     * <p>
     * A resizable popup writes an explicit preferred size onto its content -
     * that is how it remembers a width the tester dragged - and an explicit one
     * is what {@code getPreferredSize} then answers with, forever. Asking the
     * layout instead is what the platform's own pack does, and it is the whole
     * reason a dialog that had been sized once never grew again. The explicit
     * size is put back, because it is the tester's.
     */
    private static int naturalHeightOf(final @NotNull JComponent content) {
        final @NotNull Optional<Dimension> told = content.isPreferredSizeSet()
                ? Optional.of(content.getPreferredSize())
                : Optional.empty();

        content.setPreferredSize(null);
        final int natural = content.getPreferredSize().height;
        told.ifPresent(content::setPreferredSize);

        return natural;
    }

    protected final void closeOk() {
        getPopup().closeOk(null);
    }

    // ------------------------------------------------------------------
    // Assembly.
    // ------------------------------------------------------------------

    // UC-INTERNAL-007, Rule-INTERNAL-059
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
     * UC-INTERNAL-007, Rule-INTERNAL-053, Rule-INTERNAL-058.
     * <p>
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

        final @NotNull StatusBarBase statusBar = new StatusBarBase(keysShown.orElseGet(() -> dto().shortcuts().toArray(StatusBarItem[]::new)));
        strip = Optional.of(statusBar);

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
     * UC-INTERNAL-007, Rule-INTERNAL-054, Rule-INTERNAL-055, Rule-INTERNAL-056.
     * <p>
     * Binds every bindable entry's key twice: on each component's focus
     * component (exact pre-multi-component semantics, overriding any inert
     * default binding the field may carry) and on the content panel for
     * whenever the focus is elsewhere inside the dialog.
     * <p>
     * The loop itself is {@link DialogKeys}, because the menu popup makes the
     * same promise from the same declaration and is not a dialog.
     */
    private void bindShortcutKeys(final @NotNull JBPanel<?> contentPanel) {
        final @NotNull List<StatusBarShortcut> declared = dto().shortcuts();

        DialogKeys.install(contentPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, declared);

        for (final DialogComponent dialogComponent : builtComponents()) {
            if (!dialogComponent.acceptsDialogKeys()) continue;

            DialogKeys.install(dialogComponent.getFocusComponent(), JComponent.WHEN_FOCUSED, declared);
        }
    }

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-060.
     * <p>
     * Every component's own submit gesture (a click on a selection, an OK
     * button) triggers the dialog's submit action.
     */
    private void bindSubmitGesture() {
        for (final DialogComponent dialogComponent : builtComponents()) {
            dialogComponent.onSubmitRequest(this::submit);
        }
    }
}
