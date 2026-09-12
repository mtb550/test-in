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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import org.testin.util.Bundle;
import javax.swing.*;
import java.util.function.Function;

/**
 * The tabs of the Testin tool window: what each is called, and which part of the
 * panel it shows.
 * <p>
 * Both, because the window used to name the three tabs and reach for their three
 * scroll panes in three hand-written lines - so a fourth tab meant remembering a
 * place that has nothing to do with declaring one (#175, C11).
 */
@Getter
@AllArgsConstructor
public enum ViewTab {
    DETAILS(Bundle.message("view.tab.details"), ViewPanel::getDetailsScrollPane),

    // Reported as never used, and kept: the constants are read by values(), so
    // nothing names them. History draws an honest empty state until a test case
    // records more than its last edit (#150); Open Bugs reads the runs and
    // reports what each cycle found (#229).
    HISTORY(Bundle.message("view.tab.history"), ViewPanel::getHistoryScrollPane),
    OPEN_BUGS(Bundle.message("view.tab.open.bugs"), ViewPanel::getOpenBugsScrollPane);

    private final @NotNull String displayName;

    private final @NotNull Function<ViewPanel, JScrollPane> pane;

    /**
     * The part of the panel this tab shows.
     */
    public @NotNull JScrollPane paneOf(final @NotNull ViewPanel panel) {
        return pane.apply(panel);
    }
}
