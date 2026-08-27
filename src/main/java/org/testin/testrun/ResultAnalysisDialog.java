package org.testin.testrun;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.ResultAnalysis;
import org.testin.model.TestRunSummary;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextArea;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * What the tester has to say about how the run went, one paragraph per verdict.
 * <p>
 * The headings carry the counts the report will print - "Passed (10)" - so the
 * tester is writing under the same figure the reader will see, rather than
 * recalling it. The counts are read here and never stored: they are derived from
 * the results, and a stored copy would be wrong the moment a verdict changed.
 * <p>
 * A section left blank is left out of the report. Nothing has to be deleted to
 * take a paragraph back out, and a run nobody analysed prints no section at all.
 */
public final class ResultAnalysisDialog extends AbstractFrameworkDialog<TextArea> {

    private final @NotNull Map<ResultAnalysis, TextArea> written = new EnumMap<>(ResultAnalysis.class);
    private final @NotNull Consumer<@NotNull Map<ResultAnalysis, String>> onSave;

    public ResultAnalysisDialog(final @NotNull Project p, final @NotNull TestRunSummary summary, final @NotNull Map<ResultAnalysis, String> current, final @NotNull Consumer<@NotNull Map<ResultAnalysis, String>> onSave) {
        super(p);
        this.onSave = onSave;

        title = "Result Analysis";

        final @NotNull List<ComponentDialogBase<?>> parts = new ArrayList<>();

        for (final ResultAnalysis section : ResultAnalysis.values()) {
            final @NotNull ComponentDialogBase<TextArea> area = ComponentDialogBase.textArea()
                    .placeholder("what the " + section.getLabel().toLowerCase() + " cases say about this run...")
                    .value(section.writtenIn(current))
                    .rows(3)
                    .build();

            written.put(section, area.getComponent());

            parts.add(ComponentDialogBase.message(section.heading(summary)));
            parts.add(area);
        }

        // A visible button rather than an Enter hint. Enter inside a text area is
        // a new paragraph, which is what a tester writing four of them expects it
        // to be - so there is no key left to confirm with, and the dialog says so
        // with a button instead of a shortcut it would have to steal back.
        parts.add(ComponentDialogBase.button("Save"));

        components = List.copyOf(parts);

        shortcuts = List.of(
                StatusBarShortcut.hint("Tab", "Navigate"),
                StatusBarShortcut.cancel(this::closeCancel));

        // Sized rather than packed: four areas over four headings is a tall
        // dialog either way, and one that resizes as text is typed into it moves
        // the field under the tester's hands.
        preferredSize = JBUI.size(760, 640);
    }

    @Override
    protected void submit() {
        final @NotNull Map<ResultAnalysis, String> analysis = new EnumMap<>(ResultAnalysis.class);

        // Trimmed here, once, so "blank" means the same thing to the dialog that
        // saved it and to the report that decides whether to print it.
        written.forEach((section, area) -> analysis.put(section, area.getText().trim()));

        onSave.accept(analysis);
        closeOk();
    }
}
