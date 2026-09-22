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

package org.testin.editor.toolbar.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editor.AbstractIconButton;
import org.testin.editor.EditorColors;
import org.testin.editor.toolbar.Toolbar;
import org.testin.model.Automated;
import org.testin.model.Groups;
import org.testin.model.Priority;
import org.testin.model.TestStatus;
import org.testin.testcase.TestEditorAttributes;
import org.testin.util.Bundle;
import org.testin.util.Icons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class FilterPopupBtn extends AbstractIconButton implements ToolbarItem {
    @Getter
    @NotNull
    private final Set<String> selectedGroup = new HashSet<>();

    @Getter
    @NotNull
    private final Set<Priority> selectedPriority = new HashSet<>();

    @Getter
    @NotNull
    private final Set<String> selectedModule = new HashSet<>();

    @Getter
    @NotNull
    private final Set<TestStatus> selectedStatus = new HashSet<>();

    @Getter
    private final Set<Automated> selectedAutomation = new HashSet<>();

    @NotNull
    private final Supplier<Set<String>> availableModulesSupplier;

    private final Supplier<Set<String>> availableGroupsSupplier;

    @NotNull
    private final DefaultActionGroup cachedActionGroup;

    @NotNull
    private final Runnable onToolBarFilterReset;

    @NotNull
    private final Toolbar callbacks;

    public FilterPopupBtn(final @NotNull Toolbar callbacks, final @NotNull Runnable onToolBarFilterReset, final @NotNull Runnable onToolBarFilterSelectedChanged, final @NotNull Supplier<Set<String>> availableModulesSupplier, final @NotNull Supplier<Set<String>> availableGroupsSupplier) {
        super(Bundle.message("filter.button"), AllIcons.General.Filter);
        this.callbacks = callbacks;
        this.onToolBarFilterReset = onToolBarFilterReset;

        this.availableModulesSupplier = availableModulesSupplier;
        this.availableGroupsSupplier = availableGroupsSupplier;

        this.cachedActionGroup = buildActionGroup(onToolBarFilterSelectedChanged);

        addActionListener(_ -> showFilterPopup());
        updateToolBarFilterState();
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-098
    private static @NotNull AnAction nothingToFilterOn(final @NotNull String text) {
        return new DumbAwareAction(text) {
            @Override
            public void update(final @NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(false);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }

            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
            }
        };
    }

    private @NotNull List<Set<?>> filters() {
        return List.of(selectedPriority, selectedGroup, selectedModule, selectedStatus, selectedAutomation);
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-098
    public int activeFilterCount() {
        return filters().stream().mapToInt(Set::size).sum();
    }

    public boolean hasActiveFilters() {
        return activeFilterCount() > 0;
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-009
    public void updateToolBarFilterState() {
        final int activeFiltersCount = activeFilterCount();

        setOn(activeFiltersCount > 0);

        if (activeFiltersCount == 0) {
            setText(null);
            describe(Bundle.message("filter.button"));
            setForeground(JBColor.foreground());
        } else {
            setText("(" + activeFiltersCount + ")");
            describe(Bundle.message("filter.button.active", String.valueOf(activeFiltersCount)));
            setForeground(EditorColors.FILTER_ACTIVE);
        }
    }

    // UC-EDITOR-PANEL-021
    public void resetToolBarFilter() {
        clearFilters();
        onToolBarFilterReset.run();
    }

    // UC-EDITOR-PANEL-021, Rule-EDITOR-PANEL-099
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

        filterResetBtn.add(new DumbAwareAction(Bundle.message("filter.reset"), Bundle.message("filter.reset.description"), AllIcons.Actions.Cancel) {
            @Override
            public void update(final @NotNull AnActionEvent e) {
                final boolean anyFilter = hasActiveFilters();

                e.getPresentation().setVisible(true);
                e.getPresentation().setEnabled(anyFilter);
                e.getPresentation().setText(anyFilter ? Bundle.message("filter.reset") : Bundle.message("filter.reset.nothing"));
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }

            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                resetToolBarFilter();
            }
        });
        filterResetBtn.addSeparator();

        final @NotNull DefaultActionGroup filterPriorityMenu = new DefaultActionGroup(TestEditorAttributes.PRIORITY.getName(), true);
        Arrays.stream(Priority.values()).forEach(p ->
                filterPriorityMenu.add(new ToggleFilterAction<>(p.getLabel(), Icons.dot(p.getColor()),
                        p, selectedPriority, FilterMembership.plain(), onChanged)));
        filterResetBtn.add(filterPriorityMenu);

        final @NotNull DefaultActionGroup filterAutomationMenu = new DefaultActionGroup(Bundle.message("filter.automation"), true);
        Automated.FILTERABLE.forEach(a -> filterAutomationMenu.add(new ToggleFilterAction<>(a.getLabel(), a.getIcon(),
                a, selectedAutomation, FilterMembership.plain(), onChanged)));
        filterResetBtn.add(filterAutomationMenu);

        final @NotNull ActionGroup filterGroupMenu = new ActionGroup(TestEditorAttributes.GROUP.getName(), true) {
            // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-095
            @Override
            public AnAction @NotNull [] getChildren(final @Nullable AnActionEvent e) {
                final @NotNull List<AnAction> actions = new ArrayList<>();
                actions.add(new ToggleFilterAction<>(Groups.NONE, null,
                        Groups.NONE, selectedGroup, FilterMembership.plain(), onChanged));

                availableGroupsSupplier.get().stream().sorted().forEach(group ->
                        actions.add(new ToggleFilterAction<>(group, null,
                                group, selectedGroup, FilterMembership.plain(), onChanged)));

                return actions.toArray(new AnAction[0]);
            }
        };
        filterResetBtn.add(filterGroupMenu);

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

        if (callbacks.hasRunStatuses()) {
            final @NotNull DefaultActionGroup filterStatusMenu = new DefaultActionGroup(TestEditorAttributes.STATUS.getName(), true);
            Arrays.stream(TestStatus.values()).forEach(s ->
                    filterStatusMenu.add(new ToggleFilterAction<>(s.getLabel(), null,
                            s, selectedStatus, FilterMembership.plain(), onChanged)));
            filterResetBtn.add(filterStatusMenu);
        } else {
            filterResetBtn.add(nothingToFilterOn(Bundle.message("filter.status.no.run")));
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
