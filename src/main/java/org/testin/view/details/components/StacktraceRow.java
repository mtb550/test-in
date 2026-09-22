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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ui.JBUI;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.testrun.RunEditorAttributes;
import org.testin.util.Fonts;
import org.testin.ui.framework.Picture;
import org.testin.util.Bundle;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;

@AllArgsConstructor
public final class StacktraceRow extends BaseDetails {
    private static final int LINES_SHOWN = 3;

    private static final int LINK_MARGIN_TOP = 6;

    private static final int LINK_GAP = 12;

    private final @NotNull TestRunItems item;

    private final @NotNull List<String> currentPath;

    private static @NotNull JBPanel<?> line(final @NotNull List<? extends JComponent> parts) {
        final @NotNull JBPanel<?> line = new JBPanel<>(new HorizontalLayout(JBUI.scale(LINK_GAP)));
        line.setOpaque(false);
        line.setBorder(JBUI.Borders.emptyTop(LINK_MARGIN_TOP));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        parts.forEach(line::add);
        return line;
    }

    // UC-VIEW-PANEL-006, Rule-VIEW-PANEL-034, Rule-VIEW-PANEL-035, Rule-VIEW-PANEL-081
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
        if (lines.size() > LINES_SHOWN) container.add(line(List.of(showAllLink(p, dto, stacktrace, lines.size()))));
        if (!screenshots.isEmpty()) container.add(line(screenshots.stream().map(name -> thumbnail(p, name)).toList()));

        return addRow(panel, gbc, RunEditorAttributes.STACKTRACE.getName(), container, currentRow);
    }

    private @NotNull JTextArea preview(final @NotNull List<String> lines) {
        final @NotNull JTextArea area = new JTextArea(String.join("\n", lines.subList(0, Math.min(LINES_SHOWN, lines.size()))));
        area.setFont(Fonts.code());
        area.setOpaque(false);
        area.setEditable(false);
        area.setBorder(null);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        return area;
    }

    private @NotNull ActionLink showAllLink(final @NotNull Project p, final @NotNull TestCaseDto dto, final @NotNull String stacktrace, final int total) {
        return link(Bundle.message("view.stacktrace.show.all", String.valueOf(total)),
                _ -> new ErrorDetailsDialog(p, dto.getDescription(), item.getActualResult(), stacktrace).show());
    }

    // UC-VIEW-PANEL-006, Rule-VIEW-PANEL-081
    private @NotNull JComponent thumbnail(final @NotNull Project p, final @NotNull String name) {
        final @NotNull Path runPath = Services.getInstance(p, TestinRoot.class).resolve(currentPath);

        final @NotNull JBLabel square = new JBLabel(Picture.noThumbnail());
        square.setToolTipText(name);
        square.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        square.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                new ScreenshotDialog(p, name, Services.getInstance(p, ProjectIndexer.class).screenshot(runPath, name)).show();
            }
        });

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Icon thumbnail = Picture.thumbnail(Services.getInstance(p, ProjectIndexer.class).screenshot(runPath, name));
            ApplicationManager.getApplication().invokeLater(() -> square.setIcon(thumbnail));
        });

        return square;
    }
}
