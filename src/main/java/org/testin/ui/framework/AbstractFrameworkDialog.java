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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.components.JBPanel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.StatusBarItem;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.LayoutFocusTraversalPolicy;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractFrameworkDialog implements DialogHost {
    protected final @NotNull Project p;

    protected @NotNull String title = "";
    protected @NotNull List<? extends ComponentDialogBase<?>> components = List.of();
    protected @NotNull List<StatusBarShortcut> shortcuts = List.of();

    protected @NotNull Dimension preferredSize = new Dimension();

    // UC-INTERNAL-007, Rule-INTERNAL-076
    protected boolean dismissOnClickOutside;

    protected boolean resizable;

    private @NotNull Optional<DialogDto> dto = Optional.empty();
    private @NotNull Optional<List<DialogComponent>> built = Optional.empty();
    private @NotNull Optional<JBPopup> popup = Optional.empty();
    private @NotNull Optional<JComponent> content = Optional.empty();

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

    private static int naturalHeightOf(final @NotNull JComponent content) {
        final @NotNull Optional<Dimension> told = content.isPreferredSizeSet()
                ? Optional.of(content.getPreferredSize())
                : Optional.empty();

        content.setPreferredSize(null);
        final int natural = content.getPreferredSize().height;
        told.ifPresent(content::setPreferredSize);

        return natural;
    }

    protected abstract void submit();

    // UC-INTERNAL-007, Rule-INTERNAL-067
    protected final @NotNull String accepted(final @NotNull TextValue field) {
        final @NotNull String value = field.getText().trim();
        if (value.isEmpty()) field.showEmptyWarning();

        return value;
    }

    // UC-INTERNAL-007, Rule-INTERNAL-067
    protected final @NotNull String accepted(final @NotNull TextValue field, final @NotNull Predicate<String> allows, final @NotNull Refused refusal) {
        return accepted(field, value -> allows.test(value) ? Optional.empty() : Optional.of(refusal));
    }

    // UC-INTERNAL-007, Rule-INTERNAL-067
    protected final @NotNull String accepted(final @NotNull TextValue field, final @NotNull Function<String, Optional<Refused>> refusing) {
        final @NotNull String value = accepted(field);
        if (value.isEmpty()) return value;

        final @NotNull Optional<Refused> refusal = refusing.apply(value);
        if (refusal.isEmpty()) return value;

        Services.getInstance(p, Notifier.class).softRefuse(p, refusal.orElseThrow(), value);
        return "";
    }

    // UC-INTERNAL-007, Rule-INTERNAL-061, Rule-INTERNAL-075
    public final boolean show() {
        final @NotNull OpenDialogs open = Services.getInstance(p, OpenDialogs.class);

        final @NotNull Optional<JBPopup> already = open.shown(getClass());
        if (already.isPresent()) {
            if (!replacesItsKind()) {
                already.orElseThrow().getContent().requestFocusInWindow();
                return false;
            }

            already.orElseThrow().cancel();
        }

        if (popup.isEmpty()) {
            popup = Optional.of(buildPopup());
        } else if (getPopup().isDisposed()) {
            throw new IllegalStateException("This dialog was already shown and closed - create a new instance");
        }

        open.remember(getClass(), getPopup());
        getPopup().showCenteredInCurrentWindow(p);
        return true;
    }

    public final void onClosed(final @NotNull Runnable action) {
        getPopup().addListener(new JBPopupListener() {
            @Override
            public void onClosed(final @NotNull LightweightWindowEvent event) {
                action.run();
            }
        });
    }

    // UC-INTERNAL-007, Rule-INTERNAL-075
    protected boolean replacesItsKind() {
        return false;
    }

    private @NotNull JBPopup buildPopup() {
        final @NotNull JBPanel<?> contentPanel = buildContentPanel();
        content = Optional.of(contentPanel);

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

    protected void closed() {
    }

    // UC-INTERNAL-007
    protected final void showKeys(final StatusBarItem @NotNull [] items) {
        keysShown = Optional.of(items);
        strip.ifPresent(bar -> bar.updateItems(items));
    }

    // Rule-INTERNAL-097
    @Override
    public final @NotNull JComponent root() {
        return content.orElseThrow();
    }

    // UC-INTERNAL-007, Rule-INTERNAL-054
    @Override
    public final void registerShortcut(final @NotNull JComponent component, final @NotNull CustomShortcutSet shortcutSet, final @NotNull Runnable action) {
        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                action.run();
            }

            @Override
            public void update(final @NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(!claimedElsewhere(shortcutSet));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        }.registerCustomShortcutSet(shortcutSet, component);
    }

    protected boolean claimedElsewhere(final @NotNull CustomShortcutSet shortcutSet) {
        return false;
    }

    @Override
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

    protected final void closeOk() {
        getPopup().closeOk(null);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-059
    protected final void closeCancel() {
        getPopup().cancel();
    }

    protected final @NotNull JBPopup getPopup() {
        return popup.orElseThrow(() -> new IllegalStateException("Dialog popup is created on first show()"));
    }

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

    // UC-INTERNAL-007, Rule-INTERNAL-053, Rule-INTERNAL-058
    private @NotNull JBPanel<?> buildContentPanel() {
        final @NotNull List<DialogComponent> all = builtComponents();

        int fillIndex = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).fillsSpace()) fillIndex = i;
        }

        for (int i = all.size() - 1; i >= 0 && fillIndex < 0; i--) {
            if (all.get(i).canFillSpace()) fillIndex = i;
        }

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

        contentPanel.setFocusCycleRoot(true);
        contentPanel.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());

        return contentPanel;
    }

    // UC-INTERNAL-007, Rule-INTERNAL-054, Rule-INTERNAL-055, Rule-INTERNAL-056
    private void bindShortcutKeys(final @NotNull JBPanel<?> contentPanel) {
        final @NotNull List<StatusBarShortcut> declared = dto().shortcuts();

        DialogKeys.install(contentPanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, declared);

        for (final DialogComponent dialogComponent : builtComponents()) {
            if (!dialogComponent.acceptsDialogKeys()) continue;

            DialogKeys.install(dialogComponent.getFocusComponent(), JComponent.WHEN_FOCUSED, declared);
        }
    }

    // UC-INTERNAL-007, Rule-INTERNAL-060
    private void bindSubmitGesture() {
        for (final DialogComponent dialogComponent : builtComponents()) {
            dialogComponent.hostedBy(this, this::submit);
        }
    }
}
