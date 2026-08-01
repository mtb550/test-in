package org.testin.util.reports;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.FileTypes;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TestRunReport {
    private final Project project;
    private final TestRunDirectoryDto tr;

    public TestRunReport(final @NotNull Project project, final TestRunDirectoryDto tr) {
        this.project = project;
        this.tr = tr;
    }

    public TestRunReport build() {
        return this;
    }

    public void asHtml() {
        processAndSave(FileTypes.HTML);
    }

    public void asPdf() {
        processAndSave(FileTypes.PDF);
    }

    public void asExcel() {
        processAndSave(FileTypes.XLSX);
    }

    public void asJson() {
        // TODO: to be implemented
    }

    public void asXml() {
        // TODO: to be implemented
    }

    private void processAndSave(final FileTypes format) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path dirPath = tr.getPath();
                String folderName = dirPath.getFileName().toString();

                final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);
                TestRunDto runData = indexer.getTestRunByPath(dirPath);

                Map<UUID, TestCaseDto> detailsMap = fetchTestCaseDetails(runData);

                byte[] fileBytes = format.generateReport(project, tr, runData, detailsMap);

                String cleanName = runData.getRunName().replace(".json", "");

                String rawTimestamp = java.time.ZonedDateTime.now().format(Config.getDateFormatterPattern());
                String safeTimestamp = rawTimestamp.replace(":", "-").replace("/", "-");

                File reportFile = dirPath.resolve(cleanName + "_Report_" + safeTimestamp + format.getExtension()).toFile();

                Files.write(reportFile.toPath(), fileBytes);

                NotificationAction openAction = NotificationAction.createSimple("Open report", () ->
                        BrowserUtil.browse(reportFile.toURI().toString())
                );

                NotificationAction copyAction = new NotificationAction("Copy path") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        CopyPasteManager.getInstance().setContents(new StringSelection(reportFile.getAbsolutePath()));
                    }
                };
                copyAction.getTemplatePresentation().setIcon(AllIcons.Actions.Copy);

                Services.getInstance(project, Notifier.class).infoWithActions(project,
                        format.name() + " Report Generated",
                        "Saved successfully: " + reportFile.getName(),
                        openAction,
                        copyAction
                );

            } catch (final Exception ex) {
                Services.getInstance(project, Notifier.class).error(project, "Report Error", "Failed to generate " + format.name() + " report: " + ex.getMessage());
                Logger.error("Exception: " + ex.getMessage());
            }
        });
    }

    private Map<UUID, TestCaseDto> fetchTestCaseDetails(final TestRunDto tr) {
        final Map<UUID, TestCaseDto> detailsMap = new ConcurrentHashMap<>();

        if (tr.getResults().isEmpty()) {
            return detailsMap;
        }

        final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);

        for (final TestRunItems item : tr.getResults()) {
            final TestCaseDto tc = indexer.getTestCaseById(item.getId());
            detailsMap.put(item.getId(), tc);
        }

        return detailsMap;
    }
}