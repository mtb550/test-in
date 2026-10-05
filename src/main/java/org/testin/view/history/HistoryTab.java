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

package org.testin.view.history;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.git.FieldChange;
import org.testin.git.history.History;
import org.testin.git.history.HistoryEntry;
import org.testin.git.history.HistoryEntryKind;
import org.testin.git.history.TestCaseHistory;
import org.testin.indexer.TestCases;
import org.testin.model.RunItemStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.ui.Badge;
import org.testin.ui.Badges;
import org.testin.ui.Pill;
import org.testin.ui.Tooltip;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.Fonts;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class HistoryTab {
    private static final @NotNull String REQUEST = "testin.history.request";
    private static final int READING_AFTER_MILLIS = 300;
    private static final int GROUP = 50;
    private static final int GAP = 12;
    private static final int FIELD_WIDTH = 110;
    private static final @NotNull Color NOT_COMMITTED = JBColor.ORANGE;
    private static final @NotNull Color HASH = JBColor.LIGHT_GRAY;

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-099
    public void load(final @NotNull Project p, final @NotNull JBPanel<?> historyTab, final @NotNull Optional<TestCaseDto> shown) {
        final @NotNull Object request = new Object();
        historyTab.putClientProperty(REQUEST, request);
        historyTab.removeAll();
        historyTab.setLayout(new BorderLayout());
        historyTab.revalidate();
        historyTab.repaint();

        if (shown.isEmpty()) return;
        final @NotNull TestCaseDto tc = shown.orElseThrow();

        final @NotNull Timer reading = new Timer(READING_AFTER_MILLIS, _ -> {
            if (isCurrent(historyTab, request)) line(historyTab, Bundle.message("view.history.reading"));
        });
        reading.setRepeats(false);
        reading.start();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull History history = Services.getInstance(p, TestCases.class).testCaseFile(tc)
                    .map(file -> TestCaseHistory.read(p, file, tc))
                    .orElseGet(() -> History.failed(Bundle.message("view.history.no.file")));

            ApplicationManager.getApplication().invokeLater(() -> {
                reading.stop();
                if (isCurrent(historyTab, request)) show(p, historyTab, history, request);
            }, p.getDisposed());
        });
    }

    private static boolean isCurrent(final @NotNull JBPanel<?> historyTab, final @NotNull Object request) {
        return historyTab.getClientProperty(REQUEST) == request;
    }

    // Rule-VIEW-PANEL-100
    private static void show(final @NotNull Project p, final @NotNull JBPanel<?> historyTab, final @NotNull History history, final @NotNull Object request) {
        if (history.entries().isEmpty()) {
            line(historyTab, history.problem());
            return;
        }

        final @NotNull JBPanel<?> entriesPanel = new JBPanel<>();
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));
        entriesPanel.setOpaque(false);
        entriesPanel.setBorder(JBUI.Borders.empty(GAP, 16, 0, 16));

        historyTab.removeAll();
        historyTab.add(entriesPanel, BorderLayout.NORTH);
        draw(p, historyTab, entriesPanel, history.entries(), 0, request);
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-099, Rule-VIEW-PANEL-101
    private static void draw(final @NotNull Project p, final @NotNull JBPanel<?> historyTab, final @NotNull JBPanel<?> entriesPanel, final @NotNull List<HistoryEntry> entries, final int from, final @NotNull Object request) {
        if (!isCurrent(historyTab, request)) return;

        entries.subList(from, Math.min(entries.size(), from + GROUP)).forEach(entry -> entriesPanel.add(left(entry(entry))));
        historyTab.revalidate();
        historyTab.repaint();

        if (from + GROUP < entries.size()) {
            ApplicationManager.getApplication().invokeLater(() -> draw(p, historyTab, entriesPanel, entries, from + GROUP, request), p.getDisposed());
        }
    }

    // Rule-VIEW-PANEL-096, Rule-VIEW-PANEL-097, Rule-VIEW-PANEL-098
    private static @NotNull JComponent entry(final @NotNull HistoryEntry entry) {
        final @NotNull JBPanel<?> box = new JBPanel<>();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.setBorder(JBUI.Borders.compound(JBUI.Borders.customLineBottom(JBColor.border()), JBUI.Borders.empty(8, 0, 10, 0)));

        box.add(left(head(entry)));
        rows(entry).forEach(row -> box.add(left(row)));
        return box;
    }

    private static @NotNull JComponent head(final @NotNull HistoryEntry entry) {
        final @NotNull JBPanel<?> head = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 0));
        head.setOpaque(false);

        final @NotNull List<JComponent> parts = entry.isCommitted()
                ? List.of(muted(Display.formatDate(entry.when())), strong(entry.who()), hash(entry), muted(entry.message()))
                : List.of(pill(new Pill(Bundle.message("view.history.not.committed"), NOT_COMMITTED)), strong(entry.who()), muted(Display.formatDate(entry.when())));
        parts.stream().filter(part -> !(part instanceof JBLabel label) || !label.getText().isBlank()).forEach(head::add);
        return head;
    }

    // Rule-VIEW-PANEL-097
    private static @NotNull JComponent hash(final @NotNull HistoryEntry entry) {
        final @NotNull JComponent hash = pill(new Pill(entry.shortHash(), HASH));
        Tooltip.set(hash, entry.hash());
        return hash;
    }

    // Rule-VIEW-PANEL-096
    private static @NotNull List<JComponent> rows(final @NotNull HistoryEntry entry) {
        if (entry.kind() == HistoryEntryKind.UNREADABLE) return List.of(muted(Bundle.message("view.history.unreadable")));
        if (entry.kind() == HistoryEntryKind.UNCOMPARED) return List.of(muted(Bundle.message("view.history.uncompared")));
        if (entry.kind() == HistoryEntryKind.CREATED) return List.of(field(Bundle.message("caption.test.case"), pill(new Pill(Bundle.message("view.history.created"), RunItemStatus.PASSED.getRowColor()))));
        if (entry.changes().isEmpty()) return List.of(muted(Bundle.message("git.change.reordered")));

        return entry.changes().stream().map(change -> field(change.fieldName(), change(change))).toList();
    }

    private static @NotNull JComponent change(final @NotNull FieldChange change) {
        final @NotNull JBPanel<?> values = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 6, 0));
        values.setOpaque(false);

        final @NotNull JBLabel was = text(change.oldValue(), Fonts.body().deriveFont(Map.of(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON)), RunItemStatus.FAILED.getRowColor());
        Stream.of(was, muted("→"), text(change.newValue(), Fonts.body(), RunItemStatus.PASSED.getRowColor()))
                .filter(label -> !label.getText().isBlank())
                .forEach(values::add);
        return values;
    }

    private static @NotNull JComponent field(final @NotNull String name, final @NotNull JComponent value) {
        final @NotNull JBPanel<?> field = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 2));
        field.setOpaque(false);

        final @NotNull JBLabel caption = muted(name);
        caption.setPreferredSize(new Dimension(JBUI.scale(FIELD_WIDTH), caption.getPreferredSize().height));
        field.add(caption);
        field.add(value);
        return field;
    }

    private static @NotNull JComponent pill(final @NotNull Badge badge) {
        final @NotNull JBPanel<?> holder = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        holder.setOpaque(false);
        Badges.showBadges(holder, List.of(badge));
        return holder;
    }

    private static @NotNull JBLabel muted(final @NotNull String value) {
        return text(value, Fonts.body(), JBColor.GRAY);
    }

    private static @NotNull JBLabel strong(final @NotNull String value) {
        return text(value, Fonts.strong(), UIUtil.getLabelForeground());
    }

    private static @NotNull JBLabel text(final @NotNull String value, final @NotNull Font font, final @NotNull Color color) {
        final @NotNull JBLabel label = new JBLabel(value);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    private static void line(final @NotNull JBPanel<?> historyTab, final @NotNull String message) {
        historyTab.removeAll();

        final @NotNull JBLabel line = new JBLabel(message, SwingConstants.CENTER);
        line.setForeground(UIUtil.getContextHelpForeground());
        line.setBorder(JBUI.Borders.empty(20));
        historyTab.add(line, BorderLayout.CENTER);

        historyTab.revalidate();
        historyTab.repaint();
    }

    private static @NotNull JComponent left(final @NotNull JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }
}
