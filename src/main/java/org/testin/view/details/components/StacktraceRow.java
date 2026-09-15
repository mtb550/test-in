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

package org.testin.view.details.components;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.testrun.RunEditorAttributes;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.Bundle;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The first few lines of a failure's stacktrace, and the links to the rest: the
 * whole text, and each screenshot pasted with it.
 * <p>
 * Its own row type rather than a {@link RunAttributeRow} that checks which
 * attribute it is holding: every other run value is a word or a sentence, and
 * this one is fifty lines of framework plumbing around the two that name the
 * tester's own code. Given the whole panel it would be the whole panel.
 * <p>
 * Three lines because that is what the top of a stacktrace is worth: the frame
 * that threw, and enough beneath it to recognize where. Everything else is read
 * in {@link ErrorDetailsDialog}, once, by a tester who has decided they need it.
 */
@AllArgsConstructor
public final class StacktraceRow extends BaseDetails {

    /**
     * How much of the trace the panel shows before handing over to the dialog.
     */
    private static final int LINES_SHOWN = 3;

    private static final int LINK_MARGIN_TOP = 6;

    private static final int LINK_GAP = 12;

    private final @NotNull TestRunItems item;

    /**
     * The run the item belongs to, as the panel names it - where its screenshot
     * files are read from when a link is clicked.
     */
    private final @NotNull List<String> currentPath;


    /**
     * UC-VIEW-PANEL-006, Rule-VIEW-PANEL-034, Rule-VIEW-PANEL-035, Rule-VIEW-PANEL-081.
     * <p>
     * A case with nothing to explain draws no row - the same rule every other
     * run row follows, and the reason a passing case shows none of them. A
     * failure with screenshots and no text still has something to show.
     */
    @Override
    public int render(final @NotNull Project p, final @NotNull JBPanel<?> panel, final @NotNull GridBagConstraints gbc, final @NotNull TestCaseDto dto, final int currentRow) {
        final @NotNull String stacktrace = item.getStacktrace();
        final @NotNull List<String> screenshots = item.getScreenshots();
        if (stacktrace.isBlank() && screenshots.isEmpty()) return currentRow;

        final @NotNull List<String> lines = stacktrace.lines().toList();

        final @NotNull JBPanel<?> container = new JBPanel<>();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);

        if (!stacktrace.isBlank()) container.add(preview(lines));

        final @NotNull List<ActionLink> links = new ArrayList<>();
        if (lines.size() > LINES_SHOWN) links.add(showAllLink(p, dto, stacktrace, lines.size()));
        screenshots.forEach(name -> links.add(screenshotLink(p, name)));
        if (!links.isEmpty()) container.add(linkLine(links));

        return addRow(panel, gbc, RunEditorAttributes.STACKTRACE.getName(), container, currentRow);
    }

    /**
     * Monospaced and not wrapped. A stacktrace read in a proportional font
     * loses the indentation that makes it scannable, and wrapping one long
     * frame across two lines would spend a third of the preview on it.
     */
    private @NotNull JTextArea preview(final @NotNull List<String> lines) {
        final @NotNull JTextArea area = new JTextArea(String.join("\n", lines.subList(0, Math.min(LINES_SHOWN, lines.size()))));
        area.setFont(JBFont.create(new Font(Font.MONOSPACED, Font.PLAIN, (int) getValueFontSize())));
        area.setOpaque(false);
        area.setEditable(false);
        area.setBorder(null);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        return area;
    }

    /**
     * Named with the count rather than "more", so a tester knows before
     * clicking whether the rest is two lines or eighty.
     */
    private @NotNull ActionLink showAllLink(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull String stacktrace, final int total) {
        return link(Bundle.message("view.stacktrace.show.all", String.valueOf(total)),
                event -> new ErrorDetailsDialog(p, dto.getDescription(), item.getActualResult(), stacktrace).show());
    }

    /**
     * UC-VIEW-PANEL-006, Rule-VIEW-PANEL-081.
     * <p>
     * One screenshot, named as its file is, and opened at its real size in a
     * window of its own: the panel itself draws no picture. Read from its file
     * when the link is clicked, not when the panel draws (#313).
     */
    private @NotNull ActionLink screenshotLink(final @NotNull Project p, final @NotNull String name) {
        return link("[" + name + "]", event -> {
            final @NotNull Path runPath = Services.getInstance(p, TestinRoot.class).resolve(currentPath);
            new ScreenshotDialog(p, name, Services.getInstance(p, ProjectIndexer.class).screenshot(runPath, name)).show();
        });
    }

    /**
     * One line under the preview: Show all first, then the screenshots.
     */
    private static @NotNull JBPanel<?> linkLine(final @NotNull List<ActionLink> links) {
        final @NotNull JBPanel<?> line = new JBPanel<>(new HorizontalLayout(JBUI.scale(LINK_GAP)));
        line.setOpaque(false);
        line.setBorder(JBUI.Borders.emptyTop(LINK_MARGIN_TOP));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        links.forEach(line::add);
        return line;
    }
}
