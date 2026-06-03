package org.testin.util.reports;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.util.Mapper;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;

import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class TestRunReport {
    private final TestRunDirectoryDto tr;

    public TestRunReport(final TestRunDirectoryDto tr) {
        this.tr = tr;
    }

    public TestRunReport build() {
        return this;
    }

    public void asHtml() {
        processAndSave(ReportFormat.HTML);
    }

    public void asPdf() {
        processAndSave(ReportFormat.PDF);
    }

    public void asExcel() {
        processAndSave(ReportFormat.EXCEL);
    }

    public void asJson() {
        // TODO: to be implemented
    }

    public void asXml() {
        // TODO: to be implemented
    }

    private void processAndSave(final ReportFormat format) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path dirPath = tr.getPath();
                String folderName = dirPath.getFileName().toString();
                File jsonFile = dirPath.resolve(folderName + ".json").toFile();

                if (!jsonFile.exists() || !jsonFile.isFile()) {
                    Notifier.getInstance().error("Report Error", "JSON data file not found: " + jsonFile.getAbsolutePath());
                    return;
                }

                TestRunDto runData = Mapper.readValue(jsonFile, TestRunDto.class);

                Map<UUID, TestCaseDto> detailsMap = fetchTestCaseDetails(runData);

                byte[] fileBytes;

                switch (format) {
                    case HTML -> {
                        String reportHtml = new HtmlGenerator().generate(runData, detailsMap);
                        fileBytes = reportHtml.getBytes(StandardCharsets.UTF_8);
                    }
                    case PDF -> fileBytes = new PdfGenerator().generate(runData, detailsMap);
                    case EXCEL -> fileBytes = new ExcelGenerator().generate(runData, detailsMap);
                    default -> throw new UnsupportedOperationException("Unknown format: " + format.name());
                }

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

                Notifier.getInstance().infoWithActions(
                        format.name() + " Report Generated",
                        "Saved successfully: " + reportFile.getName(),
                        openAction,
                        copyAction
                );

            } catch (Exception e) {
                Notifier.getInstance().error("Report Error", "Failed to generate " + format.name() + " report: " + e.getMessage());
                Log.error("Exception: " + e.getMessage());
            }
        });
    }

    private Map<UUID, TestCaseDto> fetchTestCaseDetails(final TestRunDto tr) {
        Map<UUID, TestCaseDto> detailsMap = new ConcurrentHashMap<>();

        if (tr.getResults().isEmpty()) {
            return detailsMap;
        }

        Map<List<String>, List<UUID>> pathMap = new HashMap<>();
        for (TestRunItems item : tr.getResults()) {
            pathMap.computeIfAbsent(item.getPath(), k -> new ArrayList<>()).add(item.getId());
        }

        for (Map.Entry<List<String>, List<UUID>> entry : pathMap.entrySet()) {

            Path dirPath = org.testin.util.Tools.getInstance().buildLocalPathFromList(entry.getKey());
            List<UUID> targetIds = entry.getValue();

            if (dirPath == null || !Files.exists(dirPath) || targetIds.isEmpty()) {
                continue;
            }

            Set<UUID> idsToFind = new HashSet<>(targetIds);

            try (Stream<Path> paths = Files.list(dirPath)) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .parallel()
                        .forEach(p -> {
                            try {
                                TestCaseDto tc = Mapper.readValue(p.toFile(), TestCaseDto.class);
                                if (idsToFind.contains(tc.getId())) {
                                    detailsMap.put(tc.getId(), tc);
                                }
                            } catch (Exception ignored) {
                            }
                        });
            } catch (Exception e) {
                Log.error("Failed to load details from path " + dirPath + ": " + e.getMessage());
            }
        }
        return detailsMap;
    }

    @Getter // todo, move to separate class
    public enum ReportFormat {
        HTML(".html"),
        PDF(".pdf"),
        EXCEL(".xlsx"),
        JSON(".json"),
        XML(".xml");

        private final String extension;

        ReportFormat(String extension) {
            this.extension = extension;
        }

    }
}