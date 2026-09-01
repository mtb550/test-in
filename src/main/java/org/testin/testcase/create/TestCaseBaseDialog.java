package org.testin.testcase.create;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ui.UIUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.statusbar.StatusBarItem;
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
     * Re-measures the popup around a section that just grew or shrank, and
     * scrolls whatever holds focus back into view.
     * <p>
     * Does nothing before the popup exists: a section's fillData can fire this
     * while the dialog is still being built.
     */
    protected final void repack() {
        popup.ifPresent(open -> {
            open.pack(false, true);

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
        this.stepsSection = new StepsSection(p, dialogDisposable);
        this.prioritySection = new PrioritySection();
        this.groupSection = new GroupSection();
        this.orderSection = new OrderSection(p);
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

    public void registerShortcut(final @NotNull JComponent component, final @NotNull CustomShortcutSet shortcutSet, final @NotNull UIAction action) {
        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                action.execute();
            }

            @Override
            public void update(final @NotNull AnActionEvent e) {
                if (completionIsOpen()) {
                    e.getPresentation().setEnabled(false);
                    return;
                }
                if (prioritySection.getCombo().isPopupVisible()) {
                    e.getPresentation().setEnabled(false);
                    return;
                }

                e.getPresentation().setEnabled(true);
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