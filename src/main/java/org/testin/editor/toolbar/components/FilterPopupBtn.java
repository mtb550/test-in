package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import org.testin.editor.EditorColors;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editor.toolbar.Toolbar;
import org.testin.model.Automated;
import org.testin.model.Group;
import org.testin.model.Priority;
import org.testin.model.TestEditorAttributes;
import org.testin.model.TestStatus;
import org.testin.util.IconManager;

import java.util.*;
import java.util.function.Supplier;

public class FilterPopupBtn extends AbstractIconButton implements ToolbarItem {
    @Getter
    @NotNull
    private final Set<Group> selectedGroup = new HashSet<>();

    @Getter
    @NotNull
    private final Set<Priority> selectedPriority = new HashSet<>();

    @Getter
    @NotNull
    private final Set<String> selectedModule = new HashSet<>();

    @Getter
    @NotNull
    private final Set<TestStatus> selectedStatus = new HashSet<>();

    /**
     * Which automation states the tester is narrowing to, and empty for all of
     * them.
     */
    @Getter
    private final Set<Automated> selectedAutomation = new HashSet<>();

    @NotNull
    private final Supplier<Set<String>> availableModulesSupplier;

    @NotNull
    private final DefaultActionGroup cachedActionGroup;

    @NotNull
    private final Runnable onToolBarFilterReset;

    @NotNull
    private final Toolbar callbacks;

    public FilterPopupBtn(final @NotNull Toolbar callbacks, final @NotNull Runnable onToolBarFilterReset, final @NotNull Runnable onToolBarFilterSelectedChanged, final @NotNull Supplier<Set<String>> availableModulesSupplier) {
        super("Filter", AllIcons.General.Filter);
        this.callbacks = callbacks;
        this.onToolBarFilterReset = onToolBarFilterReset;

        this.availableModulesSupplier = availableModulesSupplier;

        this.cachedActionGroup = buildActionGroup(onToolBarFilterSelectedChanged);

        addActionListener(e -> showFilterPopup());
        updateToolBarFilterState();
    }

    /**
     * The five sets a tester can narrow the list with, as one list.
     * <p>
     * They were spelled out three times - counted for the badge, tested for the
     * Reset action, cleared on reset - so a fifth kind of filter meant finding
     * all three and a missed one showed as a badge that counted something the
     * Reset button said was not there.
     */
    private @NotNull List<Set<?>> filters() {
        return List.of(selectedPriority, selectedGroup, selectedModule, selectedStatus, selectedAutomation);
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-098
    public int activeFilterCount() {
        return filters().stream().mapToInt(Set::size).sum();
    }

    /**
     * Whether the tester has narrowed the list from this popup. Asked by the Reset
     * action, which hides itself when there is nothing to reset.
     * <p>
     * Not the same question as "is the list on screen narrowed", which the status
     * bar answers for itself by comparing the shown count with the total - that
     * one also catches a search query and a filter that happens to match
     * everything, and this one does not.
     */
    public boolean hasActiveFilters() {
        return activeFilterCount() > 0;
    }

    /**
     * UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-009.
     * <p>
     * What the button says while a filter is on: the count beside the icon, in
     * the color that means a filter is on, and the button drawn as held down.
     * <p>
     * Held down rather than merely tinted, and by the same painter light mode's
     * pin uses, because that is the look this plugin already gives a button that
     * is doing something rather than waiting to. A tester who leaves a filter on
     * and comes back to a screen missing most of its test cases has to see the
     * reason without looking for it.
     */
    public void updateToolBarFilterState() {
        final int activeFiltersCount = activeFilterCount();

        setOn(activeFiltersCount > 0);

        if (activeFiltersCount == 0) {
            setText(null);
            setToolTipText("Filter");
            setForeground(JBColor.foreground());
        } else {
            setText("(" + activeFiltersCount + ")");
            setToolTipText("Filter [" + activeFiltersCount + " active]");
            setForeground(EditorColors.FILTER_ACTIVE);
        }
    }

    // UC-EDITOR-PANEL-021
    public void resetToolBarFilter() {
        clearFilters();
        onToolBarFilterReset.run();
    }

    /**
     * UC-EDITOR-PANEL-021, Rule-EDITOR-PANEL-099.
     * <p>
     * Clears the UI state without triggering a second editor refresh.
     */
    public void clearFilters() {
        filters().forEach(Set::clear);
        updateToolBarFilterState();
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-094
    private @NotNull DefaultActionGroup buildActionGroup(final @NotNull Runnable onToolBarFilterSelectedChanged) {
        final @NotNull Runnable onChanged = () -> {
            updateToolBarFilterState();
            onToolBarFilterSelectedChanged.run();
        };

        final @NotNull DefaultActionGroup filterResetBtn = new DefaultActionGroup();

        filterResetBtn.add(new DumbAwareAction("Reset Filters", "Clear active filters", AllIcons.Actions.Cancel) {
            @Override
            public void update(final @NotNull AnActionEvent e) {
                e.getPresentation().setEnabledAndVisible(hasActiveFilters());
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                // BGT on purpose - update() reads only fields/services, never Swing state; do not switch to EDT (#52).
                return ActionUpdateThread.BGT;
            }

            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                resetToolBarFilter();
            }
        });
        filterResetBtn.addSeparator();

        // priority menu
        final @NotNull DefaultActionGroup filterPriorityMenu = new DefaultActionGroup(TestEditorAttributes.PRIORITY.getName(), true);
        Arrays.stream(Priority.values()).forEach(p ->
                filterPriorityMenu.add(new ToggleFilterAction<>(p.getLabel(), IconManager.createIcon(p.getColor()),
                        p, selectedPriority, FilterMembership.plain(), onChanged)));
        filterResetBtn.add(filterPriorityMenu);

        // automation menu: only the states a tester can act on. "Not read yet"
        // is not one of them - by the time this popup is open the answer is in,
        // and nobody can look for cases nobody has looked at.
        final @NotNull DefaultActionGroup filterAutomationMenu = new DefaultActionGroup("Automation", true);
        Automated.FILTERABLE.forEach(a -> filterAutomationMenu.add(new ToggleFilterAction<>(a.getLabel(), a.getIcon(),
                a, selectedAutomation, FilterMembership.plain(), onChanged)));
        filterResetBtn.add(filterAutomationMenu);

        // group menu
        final @NotNull DefaultActionGroup filterGroupMenu = new DefaultActionGroup(TestEditorAttributes.GROUP.getName(), true);
        Arrays.stream(Group.values()).forEach(g -> {
            if (g == Group.REGRESSION) {
                filterGroupMenu.addSeparator();
            }
            filterGroupMenu.add(new ToggleFilterAction<>(g.getName(), null,
                    g, selectedGroup, FilterMembership.plain(), onChanged));
        });
        filterResetBtn.add(filterGroupMenu);

        // module menu is dynamic: modules come from the currently loaded test cases
        final @NotNull ActionGroup filterModuleMenu = new ActionGroup(TestEditorAttributes.MODULE.getName(), true) {
            // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-095
            @Override
            public AnAction @NotNull [] getChildren(final @Nullable AnActionEvent e) {
                final @NotNull List<String> orderedModules = new ArrayList<>(availableModulesSupplier.get());
                Collections.sort(orderedModules);

                final @NotNull List<AnAction> actions = new ArrayList<>();
                for (final String module : orderedModules) {
                    actions.add(new ToggleFilterAction<>(module, null,
                            module, selectedModule, FilterMembership.plain(), onChanged));
                }
                return actions.toArray(new AnAction[0]);
            }
        };
        filterResetBtn.add(filterModuleMenu);

        // Asked of the toolbar rather than tested for a class. The event half
        // of this interface already carries defaults for "only a run does it";
        // the state half did not, so the buttons tested instead.
        if (callbacks.hasRunStatuses()) {
            final @NotNull DefaultActionGroup filterStatusMenu = new DefaultActionGroup(TestEditorAttributes.STATUS.getName(), true);
            Arrays.stream(TestStatus.values()).forEach(s ->
                    filterStatusMenu.add(new ToggleFilterAction<>(s.getLabel(), null,
                            s, selectedStatus, FilterMembership.plain(), onChanged)));
            filterResetBtn.add(filterStatusMenu);
        }

        return filterResetBtn;
    }

    private void showFilterPopup() {
        JBPopupFactory.getInstance()
                .createActionGroupPopup(null, cachedActionGroup,
                        DataManager.getInstance().getDataContext(this),
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                        true)
                .showUnderneathOf(this);
    }
}
