package org.testin.editor.toolbar.components;

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
import org.testin.editor.run.RunEditor;
import org.testin.editor.toolbar.Toolbar;
import org.testin.model.Group;
import org.testin.model.Priority;
import org.testin.model.TestEditorAttributes;
import org.testin.model.TestStatus;
import org.testin.util.IconManager;

import java.util.*;
import java.util.function.Supplier;

public class FilterPopupBtn extends AbstractButton implements ToolbarItem {
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

    public void updateToolBarFilterState() {
        final int activeFiltersCount = selectedPriority.size() + selectedGroup.size() + selectedModule.size() + selectedStatus.size();
        if (activeFiltersCount == 0) {
            setText(null);
            setToolTipText("Filter");
            setForeground(JBColor.foreground());
        } else {
            setText("(" + activeFiltersCount + ")");
            setToolTipText("Filter [" + activeFiltersCount + " active]");
            setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
        }
    }

    public void resetToolBarFilter() {
        clearFilters();
        onToolBarFilterReset.run();
    }

    /**
     * Clears the UI state without triggering a second editor refresh.
     */
    public void clearFilters() {
        selectedPriority.clear();
        selectedGroup.clear();
        selectedModule.clear();
        selectedStatus.clear();
        updateToolBarFilterState();
    }

    private @NotNull DefaultActionGroup buildActionGroup(final @NotNull Runnable onToolBarFilterSelectedChanged) {
        final Runnable onChanged = () -> {
            updateToolBarFilterState();
            onToolBarFilterSelectedChanged.run();
        };

        final DefaultActionGroup filterResetBtn = new DefaultActionGroup();

        filterResetBtn.add(new DumbAwareAction("Reset Filters", "Clear active filters", AllIcons.Actions.Cancel) {
            @Override
            public void update(final @NotNull AnActionEvent e) {
                final boolean hasActiveFilters = !selectedPriority.isEmpty() || !selectedGroup.isEmpty() || !selectedModule.isEmpty() || !selectedStatus.isEmpty();
                e.getPresentation().setEnabledAndVisible(hasActiveFilters);
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
        final DefaultActionGroup filterPriorityMenu = new DefaultActionGroup(TestEditorAttributes.PRIORITY.getName(), true);
        Arrays.stream(Priority.values()).forEach(p ->
                filterPriorityMenu.add(new ToggleFilterAction<>(p.getName(), IconManager.createIcon(p.getColor()),
                        p, selectedPriority, FilterMembership.plain(), onChanged)));
        filterResetBtn.add(filterPriorityMenu);

        // group menu
        final DefaultActionGroup filterGroupMenu = new DefaultActionGroup(TestEditorAttributes.GROUP.getName(), true);
        Arrays.stream(Group.values()).forEach(g -> {
            if (g == Group.REGRESSION) {
                filterGroupMenu.addSeparator();
            }
            filterGroupMenu.add(new ToggleFilterAction<>(g.getName(), null,
                    g, selectedGroup, FilterMembership.plain(), onChanged));
        });
        filterResetBtn.add(filterGroupMenu);

        // module menu is dynamic: modules come from the currently loaded test cases
        final ActionGroup filterModuleMenu = new ActionGroup("Module", true) {
            @Override
            public AnAction @NotNull [] getChildren(final @Nullable AnActionEvent e) {
                final List<String> orderedModules = new ArrayList<>(availableModulesSupplier.get());
                Collections.sort(orderedModules);

                final List<AnAction> actions = new ArrayList<>();
                for (final String module : orderedModules) {
                    actions.add(new ToggleFilterAction<>(module, null,
                            module, selectedModule, FilterMembership.plain(), onChanged));
                }
                return actions.toArray(new AnAction[0]);
            }
        };
        filterResetBtn.add(filterModuleMenu);

        if (callbacks instanceof RunEditor) {
            final DefaultActionGroup filterStatusMenu = new DefaultActionGroup("Status", true);
            Arrays.stream(TestStatus.values()).forEach(s ->
                    filterStatusMenu.add(new ToggleFilterAction<>(s.name(), null,
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
