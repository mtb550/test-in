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

package org.testin.report;

import org.testin.actions.GrayWithReason;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.editor.TestinEditor;
import org.testin.editor.run.RunEditor;
import org.testin.explorer.tree.TreeValues;
import org.testin.importexport.FileTypes;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.importexport.exports.ExportNotice;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.services.BackgroundWork;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.io.File;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.function.Supplier;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GenerateReportAction extends AbstractProjectAction {
    private final @NotNull Supplier<Optional<TestRunDirectoryDto>> selectedRun;

    public GenerateReportAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, Bundle.message("report.action.text"), Bundle.message("report.action.description"), AllIcons.ToolbarDecorator.Export);
        this.selectedRun = () -> TreeValues.valueOf(tree.getLastSelectedPathComponent(), TestRunDirectoryDto.class);
        registerCustomShortcutSet(Shortcuts.GenerateReport.getCustomShortcut(), tree);
    }

    public GenerateReportAction(final @NotNull Project p, final @NotNull TestinEditor editor) {
        super(p, Bundle.message("report.action.text"), Bundle.message("report.action.description"), AllIcons.Actions.Report);
        this.selectedRun = () -> editor instanceof RunEditor re ? Optional.of(re.getParent()) : Optional.empty();
    }

    public GenerateReportAction(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list) {
        this(p, editor);
        registerCustomShortcutSet(Shortcuts.GenerateReport.getCustomShortcut(), list);
    }

    // UC-REPORT-001
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute();
    }

    // UC-REPORT-001, Rule-REPORT-001
    @Override
    public void update(final @NotNull AnActionEvent e) {
        GrayWithReason.unless(this, e, isAvailable(), selectedRun.get()
                .map(tr -> Bundle.message("toolbar.report.disabled", tr.getMarker().getStatus().getLabel()))
                .orElseGet(() -> Bundle.message("report.select.run.description")));
    }

    // UC-REPORT-001, Rule-REPORT-016
    public boolean isAvailable() {
        return selectedRun.get().map(tr -> tr.getMarker().getStatus().isReportable()).orElse(false);
    }

    // UC-REPORT-001
    public void execute() {
        selectedRun.get().ifPresent(tr -> new GenerateReportDialog(p,
                ReportFileName.suggestedFor(p, tr, ZonedDateTime.now()),
                (format, file) -> processAndSave(p, tr, format, file)).show());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    // UC-REPORT-001, Rule-REPORT-003
    private void processAndSave(final @NotNull Project p, final @NotNull TestRunDirectoryDto tr, final @NotNull FileTypes format, final @NotNull File outputFile) {
        BackgroundWork.run(p, Bundle.message("report.task.generating", format.getLabel(), tr.getName()), Bundle.message("report.failed.title", format.getLabel()), indicator -> {
            final @NotNull Path dirPath = tr.getPath();

            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            final @NotNull TestRunDto runData = indexer.getTestRunByPath(dirPath);

            final @NotNull Map<UUID, TestCaseDto> detailsMap = fetchTestCaseDetails(p, runData);
            indicator.checkCanceled();

            final byte[] fileBytes = format.generateReport(p, tr, runData, detailsMap);

            // Rule-REPORT-003
            indicator.checkCanceled();

            write(outputFile, fileBytes);

            final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);

            notifier.infoWithActions(p,
                    Bundle.message("report.generated.title", format.getLabel()),
                    Bundle.message("report.generated.message", outputFile.getName()),
                    notifier.action(Bundle.message("report.open"), () -> ExportNotice.open(p, outputFile)),
                    ExportNotice.copyPath(p, outputFile)
            );
        });
    }

    private static void write(final @NotNull File outputFile, final byte @NotNull [] content) {
        try {
            Files.write(outputFile.toPath(), content);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private @NotNull Map<UUID, TestCaseDto> fetchTestCaseDetails(final @NotNull Project p, final @NotNull TestRunDto tr) {
        final @NotNull Map<UUID, TestCaseDto> detailsMap = new ConcurrentHashMap<>();

        if (tr.getResults().isEmpty()) {
            return detailsMap;
        }

        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        for (final TestRunItems item : tr.getResults()) {
            indexer.findTestCase(item.getId()).ifPresent(tc -> detailsMap.put(item.getId(), tc));
        }

        return detailsMap;
    }
}
