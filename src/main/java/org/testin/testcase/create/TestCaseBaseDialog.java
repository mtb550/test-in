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

package org.testin.testcase.create;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.StatusBarItem;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.testcase.UIAction;
import org.testin.testcase.UpdateTestCaseFields;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Getter
public abstract class TestCaseBaseDialog {
    /**
     * Every section is a key in the status bar mapping, both being built from
     * the same two enums. This is what a section that somehow is not would
     * show: an empty bar rather than another section's items.
     */
    @Getter(AccessLevel.NONE)
    private static final @NotNull StatusBarItem[] NO_ITEMS = new StatusBarItem[0];

    protected final @NotNull Project p;
    protected final @NotNull DescriptionSection descriptionSection;
    protected final @NotNull ExpectedResultSection expectedResultSection;
    protected final @NotNull ModuleSection moduleSection;
    protected final @NotNull TestDataSection testDataSection;
    protected final @NotNull PreConditionsSection preConditionsSection;
    protected final @NotNull PrioritySection prioritySection;
    protected final @NotNull GroupSection groupSection;
    protected final @NotNull StepsSection stepsSection;
    protected final @NotNull OrderSection orderSection;
    protected final @NotNull StatusSection statusSection;
    protected final @NotNull StatusBarSection statusBarSection;
    /**
     * Owns all global registrations of this dialog (application focus listener,
     * per-step shortcuts). Parented to the project, so everything is released
     * even when the popup is torn down without firing onClosed.
     */
    protected final @NotNull Disposable dialogDisposable;
    protected final @NotNull Map<CreateTestCaseSection, StatusBarItem[]> statusBarMapping;
    private final @NotNull List<CreateTestCaseSection> cachedSections;
    /**
     * The dialog's popup, empty until the constructor has finished building it.
     * <p>
     * Here rather than in each dialog. Both build a component popup, both repack
     * it when a section grows, and both show it centred - and they had already
     * drifted three ways: one guarded the repack with an if-block and the other
     * with an early return, one showed the popup only if it existed while the
     * other threw if it did not (#71).
     */
    @Getter(AccessLevel.NONE)
    private @NotNull Optional<JBPopup> popup = Optional.empty();

    /**
     * Takes ownership of the popup the subclass just built, and hands it back so
     * the constructor can go on using it.
     */
    protected final @NotNull JBPopup ownPopup(final @NotNull JBPopup built) {
        popup = Optional.of(built);
        return built;
    }

    /**
     * What a component would ask for if nobody had told it a size.
     * <p>
     * A resizable popup writes an explicit preferred size onto its content -
     * that is how it remembers a width the tester dragged - and an explicit one
     * is what {@code getPreferredSize} then answers with, forever. Asking the
     * layout instead is what the platform's own pack does, and it is the whole
     * reason a dialog that had been sized once never grew again.
     * <p>
     * The explicit size is put back, because it is the tester's.
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

    /**
     * Re-sizes the popup around a section that just grew or shrank, and scrolls
     * whatever holds focus back into view.
     * <p>
     * Does nothing before the popup exists: a section's fillData can fire this
     * while the dialog is still being built.
     */
    protected final void repack() {
        popup.ifPresent(open -> {
            // The height is set rather than packed.
            //
            // pack() asks the popup to work the size out again, and on a
            // resizable popup it does not: the create dialog grew by exactly
            // nothing while the log showed it being re-packed to 61, 90, 119,
            // 148. The update dialog, which is built not resizable, grew every
            // time - the only difference between the two builders. A tester
            // dragging the dialog taller then saw the line that had been added
            // several keystrokes earlier.
            //
            // Nothing is being worked around: the height wanted is already
            // known here - the section measured it and that is why this was
            // called - so it is said rather than asked for. The width is left
            // exactly as it is, including a width the tester chose themselves.
            final int wanted = naturalHeightOf(open.getContent());
            Logger.debug("Resizing the dialog: " + open.getSize().height + " -> " + wanted);

            open.setSize(new Dimension(open.getSize().width, wanted));

            ApplicationManager.getApplication().invokeLater(() -> {
                final @NotNull Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof JComponent jComp) {
                    jComp.scrollRectToVisible(new Rectangle(0, 0, jComp.getWidth(), jComp.getHeight()));
                }
            });
        });
    }

    public void show() {
        popup.ifPresent(open -> open.showCenteredInCurrentWindow(p));
    }

    /**
     * A focus change nothing listens for, before the dynamic status bar is
     * installed.
     */
    private static final @NotNull PropertyChangeListener NOTHING_ON_FOCUS = evt -> {
    };

    /**
     * What a focus change updates the status bar with, and one that updates
     * nothing before the dynamic bar is installed. Removing a listener that was
     * never added is what the focus manager does with it: nothing.
     */
    private @NotNull PropertyChangeListener focusListener = NOTHING_ON_FOCUS;

    public TestCaseBaseDialog(final @NotNull Project p) {
        this.p = p;
        this.dialogDisposable = Disposer.newDisposable("testin.testCaseDialog");
        Disposer.register(p, dialogDisposable);

        this.descriptionSection = new DescriptionSection(p);
        this.expectedResultSection = new ExpectedResultSection(p);
        this.moduleSection = new ModuleSection(p);
        this.testDataSection = new TestDataSection(p);
        this.preConditionsSection = new PreConditionsSection(p);
        this.stepsSection = new StepsSection(p);
        this.prioritySection = new PrioritySection();
        this.groupSection = new GroupSection(p);
        this.orderSection = new OrderSection(p);
        this.statusSection = new StatusSection();
        this.statusBarSection = new StatusBarSection();

        // Every section either dialog offers, in the order the create dialog
        // lays them out and then whatever only the update menu has.
        //
        // It used to be the create dialog's fields alone, which was the same
        // list until Order arrived: a case being created has no position to
        // choose, so Order is the first field one dialog offers and the other
        // does not (#162). A section missing from here is invisible to the
        // update dialog, to the focus-to-status-bar mapping and to the save.
        this.cachedSections = Stream.concat(
                        Arrays.stream(CreateTestCaseFields.values()).map(CreateTestCaseFields::getSectionExtractor),
                        Arrays.stream(UpdateTestCaseFields.values()).map(UpdateTestCaseFields::getSectionExtractor))
                .map(extractor -> extractor.apply(this))
                .distinct()
                .toList();

        final @NotNull Map<CreateTestCaseSection, StatusBarItem[]> bars = new LinkedHashMap<>();
        for (final CreateTestCaseFields field : CreateTestCaseFields.values())
            bars.put(field.getSectionExtractor().apply(this), field.getStatusBarItems());

        // Only for the sections the create dialog has no entry for: where both
        // enums name the same section, the create dialog's bar is the one this
        // mapping has always shown.
        for (final UpdateTestCaseFields field : UpdateTestCaseFields.values())
            bars.putIfAbsent(field.getSectionExtractor().apply(this), field.getStatusBarItems());

        this.statusBarMapping = Map.copyOf(bars);
    }

    /**
     * The section the focused component belongs to. Focus can sit on the dialog
     * itself between two sections, which is no section rather than a missing one.
     */
    private @NotNull Optional<CreateTestCaseSection> sectionHolding(final @NotNull Component focusOwner) {
        return getAllSections().stream()
                .filter(section -> UIUtil.isDescendingFrom(focusOwner, section.getWrapper()))
                .findFirst();
    }

    protected void initDynamicStatusBar(final @NotNull JComponent parentPanel) {
        // Focus leaving the window arrives as no new owner at all, and a focus
        // owner outside this dialog is somebody else's business.
        focusListener = evt -> Optional.ofNullable((Component) evt.getNewValue())
                .filter(focusOwner -> UIUtil.isDescendingFrom(focusOwner, parentPanel))
                .flatMap(this::sectionHolding)
                .ifPresent(section -> statusBarSection.updateItems(statusBarMapping.getOrDefault(section, NO_ITEMS)));
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusListener);

        // Removal runs on any disposal path (popup onClosed or project teardown).
        Disposer.register(dialogDisposable, this::removeFocusListener);
    }

    private void removeFocusListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusListener);
        focusListener = NOTHING_ON_FOCUS;
    }

    public void dispose() {
        Disposer.dispose(dialogDisposable);
    }

    /**
     * The one section the tester may change, and empty when they may change all
     * of them - which is what creating a test case means.
     * <p>
     * The update dialog opens on one field and shows the others greyed out, so
     * "shown" and "may write" stopped being the same question. Only the save
     * filtered, and it filtered on shown alone, so every greyed section wrote
     * itself back over the case. One section had grown a guard of its own
     * against exactly that and the rest had not - which is how editing a case's
     * priority came to re-write its expected result as trimmed text, silently
     * changing a stored value with leading or trailing whitespace, against the
     * project's own rule that saving never reformats.
     */
    private @NotNull Optional<CreateTestCaseSection> editableSection = Optional.empty();

    /**
     * UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-035.
     * <p>
     * Greys out every section but this one, and records that only it may write.
     */
    protected void onlyEditable(final @NotNull CreateTestCaseSection target) {
        editableSection = Optional.of(target);
        getAllSections().forEach(section -> section.setEditable(section == target));
    }

    /**
     * Whether this section may write what it holds back to the test case.
     */
    private boolean mayWrite(final @NotNull CreateTestCaseSection section) {
        return section.isShown() && editableSection.map(target -> target == section).orElse(true);
    }

    public @NotNull List<CreateTestCaseSection> getAllSections() {
        return cachedSections;
    }

    /**
     * UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-201.
     * <p>
     * Binds a key in this dialog, standing it down while a popup is using it.
     * <p>
     * <b>Only the keys a popup actually uses.</b> Every binding stood down
     * while the completion lookup or a combo was open, which is right for Enter
     * and Escape - the popup owns those, and a dialog that saved itself over an
     * open suggestion list would be saving the value the tester was in the
     * middle of replacing. It is wrong for everything else: the expected-result
     * and test-data fields suggest as the tester types, so the lookup is open
     * most of the time they are in one - and CTRL+ENTER, the key that puts a
     * line break in those very fields, was dead for exactly as long (#66).
     * <p>
     * Derived from the keystroke rather than from a list of exceptions kept
     * beside it: a popup claims the plain key, never a combination. A list
     * would be right today and stale at the next binding.
     */
    public void registerShortcut(final @NotNull JComponent component, final @NotNull CustomShortcutSet shortcutSet, final @NotNull UIAction action) {
        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                action.execute();
            }

            @Override
            public void update(final @NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(!(aPopupIsOpen() && popupClaims(shortcutSet)));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        }.registerCustomShortcutSet(shortcutSet, component);
    }

    /**
     * Whether code completion is showing its popup. The platform answers with no
     * lookup when it is not, and this is the one place that reads that.
     */
    private boolean completionIsOpen() {
        return LookupManager.getInstance(p).getActiveLookup() != null;
    }

    /**
     * Whether anything is open over this dialog that reads keys before it does -
     * the completion lookup, or a section's own list.
     */
    private boolean aPopupIsOpen() {
        return completionIsOpen() || getAllSections().stream().anyMatch(CreateTestCaseSection::isPopupOpen);
    }

    /**
     * UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-201.
     * <p>
     * Whether an open popup is using this key.
     * <p>
     * It uses the plain ones - Enter to take what is highlighted, Escape to
     * close - and nothing else. A combination reaches the field underneath,
     * which is what makes CTRL+ENTER a line break while a suggestion list is up.
     */
    private static boolean popupClaims(final @NotNull CustomShortcutSet shortcutSet) {
        return Arrays.stream(shortcutSet.getShortcuts())
                .filter(KeyboardShortcut.class::isInstance)
                .map(KeyboardShortcut.class::cast)
                .map(KeyboardShortcut::getFirstKeyStroke)
                .anyMatch(stroke -> stroke.getModifiers() == 0);
    }

    // Rule-EDITOR-PANEL-029, Rule-EDITOR-PANEL-035
    public @NotNull Runnable save(final @NotNull TestCaseDto dto, final @NotNull Consumer<@NotNull TestCaseDto> onSave, final @NotNull JBPopup[] popupWrapper) {
        return () -> {
            // A section the tester never opened holds its empty defaults, and
            // writing those over the dto would erase what is already there. A
            // section shown but greyed out holds the stored value and must not
            // write it back either, because writing it back trims it. Asked here
            // rather than at the top of every applyTo method.
            final @NotNull List<CreateTestCaseSection> writers = getAllSections().stream().filter(this::mayWrite).toList();

            // Before anything is applied, not after: a section that cannot write
            // what it holds has already said so, and going on would save the
            // case unchanged and stamp it as edited anyway.
            if (!writers.stream().allMatch(CreateTestCaseSection::accepts)) return;

            writers.forEach(section -> section.applyTo(dto));

            final @NotNull String title = dto.getDescription();
            if (!descriptionSection.isShown() || !title.trim().isEmpty()) {
                onSave.accept(dto);

                popupWrapper[0].closeOk(null);

            } else
                descriptionSection.setError(true);
        };
    }

}