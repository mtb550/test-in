package org.testin.util.reports;

import com.intellij.openapi.application.ApplicationManager;
import org.testin.pojo.Config;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.util.notifications.Notifier;

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
        processAndSave("HTML", ".html");
    }

    public void asPdf() {
        processAndSave("PDF", ".pdf");
    }

    public void asXlsx() {
        processAndSave("EXCEL (xlsx)", ".xlsx");
    }

    private void processAndSave(String format, String extension) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path dirPath = tr.getPath();
                String folderName = dirPath.getFileName().toString();
                File jsonFile = dirPath.resolve(folderName + ".json").toFile();

                if (!jsonFile.exists() || !jsonFile.isFile()) {
                    Notifier.error("Report Error", "JSON data file not found: " + jsonFile.getAbsolutePath());
                    return;
                }

                TestRunDto runData = Config.getMapper().readValue(jsonFile, TestRunDto.class);

                Map<UUID, TestCaseDto> detailsMap = fetchTestCaseDetails(runData);

                byte[] fileBytes;

                switch (format) {
                    case "HTML" -> {
                        ///  todo, to be implemented, put report file types in enum class
                        String reportHtml = new HtmlGenerator().generate(runData, detailsMap);
                        fileBytes = reportHtml.getBytes(StandardCharsets.UTF_8);
                    }

                    case "PDF" -> fileBytes = new PdfGenerator().generate(runData, detailsMap);

                    case "EXCEL (xlsx)" -> fileBytes = new ExcelGenerator().generate(runData, detailsMap);

                    case null, default -> throw new UnsupportedOperationException("Unknown format: " + format);
                }

                String cleanName = runData.getRunName().replace(".json", "");
                File reportFile = dirPath.resolve(cleanName + "_Report" + extension).toFile();

                Files.write(reportFile.toPath(), fileBytes);

                Notifier.infoWithOpenAndCopy(
                        format + " Report Generated",
                        "Saved successfully: " + reportFile.getName(),
                        reportFile
                );

            } catch (Exception e) {
                Notifier.error("Report Error", "Failed to generate " + format + " report: " + e.getMessage());
            }
        });
    }

    private Map<UUID, TestCaseDto> fetchTestCaseDetails(TestRunDto metadata) {
        Map<UUID, TestCaseDto> detailsMap = new ConcurrentHashMap<>();

        if (metadata.getTestCase() == null || metadata.getTestCase().isEmpty()) {
            return detailsMap;
        }

        for (TestRunDto.TestCase tcPathObj : metadata.getTestCase()) {
            Path dirPath = tcPathObj.getPath();
            List<UUID> targetIds = tcPathObj.getUuid();

            if (dirPath == null || !Files.exists(dirPath) || targetIds == null || targetIds.isEmpty()) {
                continue;
            }

            Set<UUID> idsToFind = new HashSet<>(targetIds);

            try (Stream<Path> paths = Files.list(dirPath)) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .parallel()
                        .forEach(p -> {
                            try {
                                TestCaseDto tc = Config.getMapper().readValue(p.toFile(), TestCaseDto.class);
                                if (idsToFind.contains(tc.getId())) {
                                    detailsMap.put(tc.getId(), tc);
                                }
                            } catch (Exception ignored) {
                            }
                        });
            } catch (Exception e) {
                System.err.println("Failed to load details from path " + dirPath + ": " + e.getMessage());
            }
        }
        return detailsMap;
    }

    public void asJson() {
        // TODO: to be implemented
    }

    public void asXml() {
        // TODO: to be implemented
    }

}