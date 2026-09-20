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

package org.testin.view;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import org.testin.util.Bundle;
import org.testin.view.bugs.OpenBugsTab;
import org.testin.view.details.DetailsTab;
import org.testin.view.history.HistoryTab;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The tabs of the Testin tool window: what each is called, which part of the
 * panel it shows, and which part takes the keyboard when it comes to the front
 * (#311).
 * <p>
 * Together, because the window used to name the three tabs and reach for their three
 * scroll panes in three hand-written lines - so a fourth tab meant remembering a
 * place that has nothing to do with declaring one (#175, C11).
 * <p>
 * Filling a tab is declared here too, for the same reason. A refresh used to
 * construct all three tab classes by name and hand each one a different set of
 * arguments, so a fourth tab was a fourth line in a method that is otherwise
 * about nothing; now every tab says how it loads itself and the refresh is a walk
 * over {@link #values()}.
 */
@Getter
@AllArgsConstructor
public enum ViewTab {
    DETAILS(
            Bundle.message("view.tab.details"),
            ViewPanel::getDetailsScrollPane,
            ViewPanel::getDetailsTab,
            panel -> new DetailsTab().load(panel.getP(), panel.getDetailsTab(), panel.shownCase(), panel.getPage().getCurrentPath())
    ),

    // Reported as never used, and kept: the constants are read by values(), so
    // nothing names them. History draws an honest empty state until a test case
    // records more than its last edit (#150); Open Bugs reads the runs and
    // reports what each cycle found (#229).
    HISTORY(
            Bundle.message("view.tab.history"),
            ViewPanel::getHistoryScrollPane,
            ViewPanel::getHistoryTab,
            panel -> new HistoryTab().load(panel.getHistoryTab())
    ),

    OPEN_BUGS(
            Bundle.message("view.tab.open.bugs"),
            ViewPanel::getOpenBugsScrollPane,
            ViewPanel::getOpenBugsTab,
            panel -> new OpenBugsTab().load(panel.getP(), panel.getOpenBugsTab(), panel.shownCase())
    );

    private final @NotNull String displayName;

    private final @NotNull Function<ViewPanel, JScrollPane> pane;

    /**
     * The part that takes the keyboard when this tab comes to the front (#311).
     */
    private final @NotNull Function<ViewPanel, JComponent> keyboardTarget;

    /**
     * How this tab fills itself from the panel it belongs to.
     */
    @Getter(AccessLevel.NONE)
    private final @NotNull Consumer<ViewPanel> loader;

    /**
     * The part of the panel this tab shows.
     */
    public @NotNull JScrollPane paneOf(final @NotNull ViewPanel panel) {
        return pane.apply(panel);
    }

    /**
     * UC-VIEW-PANEL-017, Rule-VIEW-PANEL-080.
     * <p>
     * The part of the panel the keyboard goes to when this tab is in front.
     */
    public @NotNull JComponent keyboardTargetOf(final @NotNull ViewPanel panel) {
        return keyboardTarget.apply(panel);
    }

    /**
     * Rule-VIEW-PANEL-008.
     * <p>
     * Fills this tab from the panel. Every tab is loaded on a refresh, whichever
     * one is in front: the tester switches tabs without anything reloading, so a
     * tab that only filled when it came forward would show the case before last.
     */
    public void load(final @NotNull ViewPanel panel) {
        loader.accept(panel);
    }
}
