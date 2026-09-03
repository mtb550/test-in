package org.testin.view;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

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
    DETAILS("Details", ViewPanel::getDetailsScrollPane),

    // Reported as never used, and kept: these two tabs are declared and not yet
    // built, not dead. The view panel renders DETAILS only (ViewPanel:110) until
    // the history and bug views land (#61).
    HISTORY("History", ViewPanel::getHistoryScrollPane),
    OPEN_BUGS("Open Bugs", ViewPanel::getOpenBugsScrollPane);

    private final @NotNull String displayName;

    private final @NotNull Function<ViewPanel, JScrollPane> pane;

    /**
     * The part of the panel this tab shows.
     */
    public @NotNull JScrollPane paneOf(final @NotNull ViewPanel panel) {
        return pane.apply(panel);
    }
}
