package org.testin.view.bugs;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class OpenBugsTab {

    /**
     * UC-VIEW-PANEL-008, Rule-VIEW-PANEL-038.
     * <p>
     * With no test case shown there is no test case to say anything about. The
     * tab used to read "No bugs found for this test case" whatever the panel
     * was showing, so with nothing selected it described a test case that was
     * not there - and said it beside a Details tab that correctly asked the
     * tester to select one (#230).
     */
    public void load(final @NotNull JBPanel<?> bugTab, final boolean showingATestCase) {
        bugTab.removeAll();
        bugTab.add(new JBLabel(showingATestCase
                ? "No bugs found for this test case."
                : "Select a test case to view its bugs"), BorderLayout.NORTH);
    }
}