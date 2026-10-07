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
import org.testin.editor.open.TestinEditors;
import org.testin.git.change.FieldChange;
import org.testin.git.history.BugCard;
import org.testin.git.history.BugEvent;
import org.testin.git.history.BugHistory;
import org.testin.git.history.History;
import org.testin.git.history.HistoryCard;
import org.testin.git.history.HistoryEntry;
import org.testin.git.history.HistoryEntryKind;
import org.testin.git.history.TestCaseHistory;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCaseDto;
import org.testin.model.result.TestRunItems;
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.ui.Badge;
import org.testin.ui.Badges;
import org.testin.ui.Pill;
import org.testin.ui.Tooltip;
import org.testin.util.Bundle;
import org.testin.util.Display;
import org.testin.util.Fonts;
import org.testin.view.details.AbstractDetails;
import org.testin.view.details.BugIssueLink;

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
import java.nio.file.Path;
import java.util.ArrayList;
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
    private static final int BAR = 3;
    private static final @NotNull Color NOT_COMMITTED = JBColor.ORANGE;
    private static final @NotNull Color HASH = JBColor.LIGHT_GRAY;

    private static boolean isCurrent(final @NotNull JBPanel<?> historyTab, final @NotNull Object request) {
        return request.equals(historyTab.getClientProperty(REQUEST));
    }

    // Rule-VIEW-PANEL-100
    private static void show(final @NotNull Project p, final @NotNull JBPanel<?> historyTab, final @NotNull History history, final @NotNull TestCaseDto tc, final @NotNull Object request) {
        final @NotNull List<HistoryCard> cards = history.cards();
        if (cards.isEmpty()) {
            line(historyTab, history.problem());
            return;
        }

        final @NotNull JBPanel<?> entriesPanel = new JBPanel<>();
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));
        entriesPanel.setOpaque(false);
        entriesPanel.setBorder(JBUI.Borders.empty(GAP, 16, 0, 16));

        historyTab.removeAll();
        historyTab.add(entriesPanel, BorderLayout.NORTH);
        if (!history.problem().isBlank()) historyTab.add(lineLabel(history.problem()), BorderLayout.CENTER);
        draw(p, historyTab, entriesPanel, cards, tc, 0, request);
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-099, Rule-VIEW-PANEL-101
    private static void draw(final @NotNull Project p, final @NotNull JBPanel<?> historyTab, final @NotNull JBPanel<?> entriesPanel, final @NotNull List<HistoryCard> cards, final @NotNull TestCaseDto tc, final int from, final @NotNull Object request) {
        if (!isCurrent(historyTab, request)) return;

        cards.subList(from, Math.min(cards.size(), from + GROUP)).forEach(card -> entriesPanel.add(left(switch (card) {
            case HistoryEntry entry -> entry(entry);
            case BugCard bug -> bugCard(p, bug, tc);
        })));
        historyTab.revalidate();
        historyTab.repaint();

        if (from + GROUP < cards.size()) {
            ApplicationManager.getApplication().invokeLater(() -> draw(p, historyTab, entriesPanel, cards, tc, from + GROUP, request), p.getDisposed());
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
    private static @NotNull JComponent hash(final @NotNull HistoryCard entry) {
        final @NotNull JComponent hash = pill(new Pill(entry.shortHash(), HASH));
        Tooltip.set(hash, entry.hash());
        return hash;
    }

    // Rule-VIEW-PANEL-096
    private static @NotNull List<JComponent> rows(final @NotNull HistoryEntry entry) {
        if (entry.kind() == HistoryEntryKind.UNREADABLE)
            return List.of(muted(Bundle.message("view.history.unreadable")));
        if (entry.kind() == HistoryEntryKind.UNCOMPARED)
            return List.of(muted(Bundle.message("view.history.uncompared")));
        if (entry.kind() == HistoryEntryKind.CREATED)
            return List.of(field(Bundle.message("caption.test.case"), pill(new Pill(Bundle.message("view.history.created"), RunItemStatus.PASSED.getRowColor()))));
        if (entry.kind() == HistoryEntryKind.REMOVED)
            return List.of(field(Bundle.message("caption.test.case"), pill(new Pill(RunItemStatus.REMOVED.getLabel(), RunItemStatus.REMOVED.getRowColor()))));
        if (entry.changes().isEmpty()) return List.of(muted(Bundle.message("git.change.reordered")));

        return entry.changes().stream().map(change -> field(change.fieldName(), change(change))).toList();
    }

    // Rule-VIEW-PANEL-105
    private static @NotNull JComponent bugCard(final @NotNull Project p, final @NotNull BugCard bug, final @NotNull TestCaseDto tc) {
        final @NotNull BugEvent event = bug.event();
        final @NotNull Color bar = event.kind().barOf(event.item());

        final @NotNull JBPanel<?> box = new JBPanel<>();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.setBorder(JBUI.Borders.compound(JBUI.Borders.customLineBottom(JBColor.border()), JBUI.Borders.compound(JBUI.Borders.customLine(bar, 0, BAR, 0, 0), JBUI.Borders.empty(8, 8, 10, 0))));

        box.add(left(bugHead(p, bug, tc)));
        bugRows(p, event).forEach(row -> box.add(left(row)));
        return box;
    }

    private static @NotNull JComponent bugHead(final @NotNull Project p, final @NotNull BugCard bug, final @NotNull TestCaseDto tc) {
        final @NotNull JBPanel<?> head = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 0));
        head.setOpaque(false);

        final @NotNull JBPanel<?> bugIn = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 4, 0));
        bugIn.setOpaque(false);
        bugIn.add(strong(Bundle.message("view.history.bug.in")));
        bugIn.add(testRun(p, bug.event(), tc));

        final @NotNull JComponent kind = pill(new Pill(bug.event().kind().getLabel(), bug.event().kind().getColor()));
        final @NotNull List<JComponent> parts = bug.isCommitted()
                ? List.of(kind, bugIn, strong(bug.who()), muted(Display.formatDate(bug.when())), hash(bug))
                : List.of(kind, bugIn, pill(new Pill(Bundle.message("view.history.not.committed"), NOT_COMMITTED)), strong(bug.who()), muted(Display.formatDate(bug.when())));
        parts.stream().filter(part -> !(part instanceof JBLabel label) || !label.getText().isBlank()).forEach(head::add);
        return head;
    }

    // Rule-VIEW-PANEL-109
    private static @NotNull JComponent testRun(final @NotNull Project p, final @NotNull BugEvent event, final @NotNull TestCaseDto tc) {
        return Services.getInstance(p, TestRuns.class).findTestRunDir(event.testRun())
                .map(dir -> {
                    final @NotNull JComponent link = AbstractDetails.link(event.testRunName(), _ -> Services.getInstance(p, TestinEditors.class).openAndSelect(dir, tc));
                    link.setFont(Fonts.strong());
                    return link;
                })
                .orElseGet(() -> {
                    final @NotNull JBLabel gone = text(event.testRunName(), Fonts.strong(), JBColor.GRAY);
                    Tooltip.set(gone, Bundle.message("view.history.bug.test.run.gone"));
                    return gone;
                });
    }

    // Rule-VIEW-PANEL-105, Rule-VIEW-PANEL-108
    private static @NotNull List<JComponent> bugRows(final @NotNull Project p, final @NotNull BugEvent event) {
        return switch (event.kind()) {
            case RECORDED -> bugAttributes(p, event.item());
            case CHANGED -> event.changes().stream().map(change -> field(change.fieldName(), change(change))).toList();
            case CLEARED ->
                    List.of(field(Bundle.message("view.history.bug.because"), plain(Bundle.message("view.history.bug.cleared.why", event.item().getStatus().getLabel()))));
            case REMOVED -> List.of();
        };
    }

    private static @NotNull List<JComponent> bugAttributes(final @NotNull Project p, final @NotNull TestRunItems item) {
        final @NotNull List<JComponent> rows = new ArrayList<>();
        if (item.isFailed()) {
            rows.add(field(TestRunEditorAttributes.BUG_SEVERITY.getName(), text(item.getBugSeverity().getLabel(), Fonts.body(), item.getBugSeverity().getColor())));
            rows.add(field(TestRunEditorAttributes.BUG_PRIORITY.getName(), plain(item.getBugPriority().getLabel())));
        }
        item.bugIssue().ifPresent(url -> rows.add(field(TestRunEditorAttributes.BUG_ISSUE.getName(), BugIssueLink.of(p, url))));
        return rows;
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

        field.add(caption(name));
        field.add(value);
        return field;
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-111
    private static @NotNull JBLabel caption(final @NotNull String name) {
        final @NotNull JBLabel caption = UpdateTestCaseFields.iconOf(name).map(JBLabel::new).orElseGet(() -> named(name));
        Tooltip.set(caption, name);
        caption.getAccessibleContext().setAccessibleName(name);
        return caption;
    }

    private static @NotNull JBLabel named(final @NotNull String name) {
        final @NotNull JBLabel caption = muted(name);
        caption.setPreferredSize(new Dimension(JBUI.scale(FIELD_WIDTH), caption.getPreferredSize().height));
        return caption;
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

    private static @NotNull JBLabel plain(final @NotNull String value) {
        return text(value, Fonts.body(), UIUtil.getLabelForeground());
    }

    private static @NotNull JBLabel strong(final @NotNull String value) {
        return text(value, Fonts.strong(), UIUtil.getLabelForeground());
    }

    // Rule-INTERNAL-132
    private static @NotNull JBLabel text(final @NotNull String value, final @NotNull Font font, final @NotNull Color color) {
        final @NotNull JBLabel label = new JBLabel(Display.shortDates(value));
        label.setFont(font);
        label.setForeground(color);
        Tooltip.set(label, Display.dateTooltip(value));
        return label;
    }

    private static void line(final @NotNull JBPanel<?> historyTab, final @NotNull String message) {
        historyTab.removeAll();
        historyTab.add(lineLabel(message), BorderLayout.CENTER);

        historyTab.revalidate();
        historyTab.repaint();
    }

    private static @NotNull JBLabel lineLabel(final @NotNull String message) {
        final @NotNull JBLabel line = new JBLabel(message, SwingConstants.CENTER);
        line.setForeground(UIUtil.getContextHelpForeground());
        line.setBorder(JBUI.Borders.empty(20));
        return line;
    }

    private static @NotNull JComponent left(final @NotNull JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

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

        final @NotNull Map<Path, String> runItemsNow = BugHistory.runItemsNow(p, tc.getId());
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull TestCases testCases = Services.getInstance(p, TestCases.class);
            final @NotNull History history = testCases.testCaseFile(tc)
                    .or(() -> TestCaseHistory.deletedFile(p, tc.getId()))
                    .map(file -> BugHistory.addTo(TestCaseHistory.read(p, file, testCases.findTestCase(tc.getId())), p, file, tc.getId(), runItemsNow))
                    .orElseGet(() -> History.failed(Bundle.message("view.history.no.file")));

            ApplicationManager.getApplication().invokeLater(() -> {
                reading.stop();
                if (isCurrent(historyTab, request)) show(p, historyTab, history, tc, request);
            }, p.getDisposed());
        });
    }
}
