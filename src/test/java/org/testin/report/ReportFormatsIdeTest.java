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

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.WriteAction;
import com.intellij.util.ui.ImageUtil;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.testproject.BoundTestProject;
import org.testin.importexport.FileTypes;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.RunItemStatus;
import org.testin.model.TestRunItems;
import org.testin.model.TestRunSummary;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.util.Bundle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReportFormatsIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull String TEST_PROJECT = "NAFATH";

    private static final @NotNull String TEST_RUN = "Cycle-1";

    private static final @NotNull String STACKTRACE = "java.lang.AssertionError: the account stayed open";

    private static final @NotNull String SCREENSHOT = "screenshot-1.png";

    private final @NotNull UUID opens = UUID.randomUUID();

    private final @NotNull UUID locks = UUID.randomUUID();

    private final @NotNull UUID deleted = UUID.randomUUID();

    private String wasBound;

    private TestProjectDirectoryDto testProject;

    private TestSetDirectoryDto login;

    private @NotNull BoundTestProject bound() {
        return Services.getInstance(getProject(), BoundTestProject.class);
    }

    private @NotNull DirectoryMapper mapper() {
        return Services.getInstance(getProject(), DirectoryMapper.class);
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    @Override
    protected void setUp() {
        super.setUp();
        wasBound = bound().name();
        bound().choose(TEST_PROJECT);

        WriteAction.runAndWait(() -> {
            testProject = mapper().setTestProjectNode(root.resolve(TEST_PROJECT));
            nodes().addTestProject(testProject);

            login = mapper().getTestSetNode(testProject.getTestCasesDirectory().getPath().resolve("Login"), testProject.getTestCasesDirectory());
            nodes().addTestSet(login);
        });

        aTestCase(opens, "Open the app\nLog in with a valid user");
        aTestCase(locks, "Lock the account after three wrong passwords");
    }

    @Override
    protected void tearDown() {
        bound().choose(wasBound);
        super.tearDown();
    }

    private void aTestCase(final @NotNull UUID id, final @NotNull String description) {
        final @NotNull TestCaseDto tc = TestCaseDto.builder().id(id).description(description).order(id.toString()).build();
        tc.setParent(login);
        Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(login.getPath(), tc);
    }

    private @NotNull TestRunDirectoryDto aTestRun(final @NotNull String named, final @NotNull List<TestRunItems> results) {
        final @NotNull TestRunDirectoryDto testRun = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunDirectoryDto dir = mapper().setTestRunNode(testProject.getTestRunsDirectory().getPath().resolve(named), testProject.getTestRunsDirectory());
            nodes().addTestRunDir(dir);
            return dir;
        });

        Services.getInstance(getProject(), TestRuns.class).putTestRun(testRun.getPath(), new TestRunDto().setResults(new ArrayList<>(results)));
        return testRun;
    }

    private @NotNull TestRunDirectoryDto theTestRun() {
        return aTestRun(TEST_RUN, List.of(
                new TestRunItems().setId(opens).setStatus(RunItemStatus.PASSED),
                new TestRunItems().setId(locks).setStatus(RunItemStatus.FAILED)
                        .setActualResult("The account\nstayed open")
                        .setStacktrace(STACKTRACE)
                        .setScreenshots(List.of(SCREENSHOT)),
                new TestRunItems().setId(deleted).setStatus(RunItemStatus.PASSED)));
    }

    private @NotNull TestRunDto resultsOf(final @NotNull TestRunDirectoryDto testRun) {
        return Services.getInstance(getProject(), TestRuns.class).getTestRunByPath(testRun.getPath());
    }

    private byte @NotNull [] report(final @NotNull FileTypes format, final @NotNull TestRunDirectoryDto testRun) {
        return format.generateReport(getProject(), testRun, resultsOf(testRun));
    }

    private @NotNull String html(final @NotNull TestRunDirectoryDto testRun) {
        return new String(report(FileTypes.HTML, testRun), StandardCharsets.UTF_8);
    }

    private @NotNull String pdf(final @NotNull TestRunDirectoryDto testRun) {
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(report(FileTypes.PDF, testRun))))) {
            final @NotNull StringBuilder text = new StringBuilder();
            for (int page = 1; page <= document.getNumberOfPages(); page++) text.append(PdfTextExtractor.getTextFromPage(document.getPage(page))).append('\n');
            return text.toString();
        } catch (final IOException ex) {
            throw new AssertionError("Could not read the PDF back: " + ex.getMessage(), ex);
        }
    }

    private @NotNull String word(final @NotNull TestRunDirectoryDto testRun) {
        try (XWPFWordExtractor extractor = new XWPFWordExtractor(new XWPFDocument(new ByteArrayInputStream(report(FileTypes.WORD, testRun))))) {
            return extractor.getText();
        } catch (final IOException ex) {
            throw new AssertionError("Could not read the Word document back: " + ex.getMessage(), ex);
        }
    }

    private @NotNull List<List<List<String>>> sheets(final @NotNull TestRunDirectoryDto testRun) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(report(FileTypes.XLSX, testRun)))) {
            final @NotNull DataFormatter formatter = new DataFormatter();
            final @NotNull List<List<List<String>>> sheets = new ArrayList<>();

            for (final Sheet sheet : workbook) {
                final @NotNull List<List<String>> rows = new ArrayList<>();
                for (final Row row : sheet) {
                    final @NotNull List<String> cells = new ArrayList<>();
                    for (final Cell cell : row) cells.add(formatter.formatCellValue(cell));
                    rows.add(cells);
                }
                sheets.add(rows);
            }
            return sheets;
        } catch (final IOException ex) {
            throw new AssertionError("Could not read the spreadsheet back: " + ex.getMessage(), ex);
        }
    }

    private @NotNull List<String> sheetNames(final @NotNull TestRunDirectoryDto testRun) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(report(FileTypes.XLSX, testRun)))) {
            final @NotNull List<String> names = new ArrayList<>();
            for (final Sheet sheet : workbook) names.add(sheet.getSheetName());
            return names;
        } catch (final IOException ex) {
            throw new AssertionError("Could not read the spreadsheet back: " + ex.getMessage(), ex);
        }
    }

    private @NotNull String excel(final @NotNull TestRunDirectoryDto testRun) {
        final @NotNull StringBuilder text = new StringBuilder();
        sheets(testRun).forEach(sheet -> sheet.forEach(row -> text.append(String.join("\t", row)).append('\n')));
        return text.toString();
    }

    private static @NotNull String withoutTags(final @NotNull String html) {
        return html.replaceAll("<[^>]+>", "");
    }

    private static @NotNull String squeezed(final @NotNull String text) {
        return text.replaceAll("\\s", "");
    }

    private static void assertPrints(final @NotNull String format, final @NotNull String report, final @NotNull String expected) {
        assertTrue(format + " does not print: " + expected, squeezed(report).contains(squeezed(expected)));
    }

    private static void assertLeavesOut(final @NotNull String format, final @NotNull String report, final @NotNull String unexpected) {
        assertFalse(format + " prints what it should leave out: " + unexpected, report.contains(unexpected));
    }

    // Rule-REPORT-002
    public void testEveryFormatPrintsTheSameFigures() {
        final @NotNull TestRunDirectoryDto testRun = theTestRun();
        final @NotNull TestRunSummary summary = TestRunSummary.of(resultsOf(testRun).getResults());
        final @NotNull String figures = Bundle.message("report.summary.named", TEST_RUN, String.valueOf(summary.total()), String.valueOf(summary.executed()), summary.passRate() + "%");

        assertPrints("The web page", withoutTags(html(testRun)), figures);
        assertPrints("The PDF", pdf(testRun), figures);
        assertPrints("The Word document", word(testRun), figures);
        assertPrints("The spreadsheet", excel(testRun), figures);
    }

    // Rule-REPORT-005
    public void testTheProjectNamedIsTheTestProject() {
        final @NotNull TestRunDirectoryDto testRun = theTestRun();
        final @NotNull String codeProject = getProject().getName();

        for (final String report : List.of(html(testRun), pdf(testRun), word(testRun), excel(testRun))) {
            assertPrints("A report", report, TEST_PROJECT);
            assertLeavesOut("A report", report, codeProject);
        }
    }

    // Rule-REPORT-017
    public void testNoReportShowsAScreenshotAndOnlyTheWebPagePrintsTheError() {
        final @NotNull TestRunDirectoryDto testRun = theTestRun();
        final @NotNull String html = html(testRun);

        assertPrints("The web page", html, STACKTRACE);
        assertLeavesOut("The web page", html, "<img");
        assertLeavesOut("The web page", html, SCREENSHOT);

        for (final String report : List.of(pdf(testRun), word(testRun), excel(testRun))) {
            assertLeavesOut("A report", report, STACKTRACE);
            assertLeavesOut("A report", report, SCREENSHOT);
        }
    }

    // Rule-SETTING-043, Rule-SETTING-044
    public void testTheCompanyLogoTopsTheReportThirtyPointsTallAndIsLeftOutWhenItsFileIsGone() {
        final @NotNull AppSettingsState settings = Services.getInstance(AppSettingsState.class);
        final @NotNull String was = settings.companyLogo;
        try {
            final @NotNull Path gif = root.resolve("logo.gif");
            ImageIO.write(ImageUtil.createImage(60, 20, BufferedImage.TYPE_INT_ARGB), "gif", gif.toFile());
            settings.companyLogo = gif.toString();
            final @NotNull TestRunDirectoryDto testRun = theTestRun();

            final @NotNull String html = html(testRun);
            assertTrue("the web page has no company logo", html.contains("<img class='company-logo'"));
            assertTrue("the web page prints the logo below its title", html.indexOf("class='company-logo'") < html.indexOf("class='report-title'"));

            try (PdfDocument pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(report(FileTypes.PDF, testRun))))) {
                assertFalse("the PDF has no company logo", pdf.getPage(1).getResources().getResourceNames(PdfName.XObject).isEmpty());
            }

            try (XWPFDocument word = new XWPFDocument(new ByteArrayInputStream(report(FileTypes.WORD, testRun)))) {
                final @NotNull XWPFPicture logo = word.getParagraphs().getFirst().getRuns().getFirst().getEmbeddedPictures().getFirst();
                assertEquals("the logo is not 30 points tall", 30.0, logo.getDepth(), 0.5);
                assertEquals("the logo lost its proportions", 90.0, logo.getWidth(), 0.5);
            }

            settings.companyLogo = root.resolve("gone.gif").toString();
            assertFalse("a missing logo file still printed a logo", html(testRun).contains("<img class='company-logo'"));
        } catch (final IOException ex) {
            throw new AssertionError("Could not write or read back a report with a logo: " + ex.getMessage(), ex);
        } finally {
            settings.companyLogo = was;
        }
    }

    // Rule-REPORT-018
    public void testThePdfSaysWhatItCouldNotPrint() {
        aTestCase(opens, "\u0633\u062c\u0644 \u0627\u0644\u062f\u062e\u0648\u0644");
        final @NotNull TestRunDirectoryDto testRun = aTestRun("Cycle-Arabic", List.of(new TestRunItems().setId(opens).setStatus(RunItemStatus.PASSED)));

        final @NotNull List<Notification> said = new ArrayList<>();
        getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(Notifications.TOPIC, new Notifications() {
            @Override
            public void notify(final @NotNull Notification notification) {
                said.add(notification);
            }
        });

        report(FileTypes.PDF, testRun);

        Await.until("the PDF left Arabic out and said nothing", () -> said.stream().anyMatch(n -> n.getTitle().equals(Bundle.message("report.pdf.left.out.title"))));
    }

    // Rule-REPORT-019
    public void testEveryFormatNamesTheColumnsTheSameWay() {
        final @NotNull TestRunDirectoryDto testRun = theTestRun();
        final @NotNull List<String> words = List.of(Bundle.message("caption.test.case"), TestRunEditorAttributes.BUG_PRIORITY.getName(), TestRunEditorAttributes.BUG_SEVERITY.getName());

        for (final String column : words) {
            assertPrints("The web page", html(testRun), ">" + column + "</th>");
            assertPrints("The PDF", pdf(testRun), column);
            assertPrints("The Word document", word(testRun), column);
        }

        final @NotNull List<String> header = sheets(testRun).get(1).getFirst();
        for (final String column : words) assertTrue("The spreadsheet names a column differently: " + column + " is not in " + header, header.contains(column));
    }

    // Rule-REPORT-020
    public void testTheSpreadsheetHasAnOverviewSheetAndATestCasesSheet() {
        final @NotNull TestRunDirectoryDto testRun = theTestRun();

        assertEquals(List.of(Bundle.message("report.excel.sheet.overview"), Bundle.message("report.excel.sheet.test.cases")), sheetNames(testRun));

        final @NotNull List<List<String>> testCases = sheets(testRun).get(1);
        assertEquals("the Test Cases sheet holds the column names and one row per test case, and nothing else",
                resultsOf(testRun).getResults().size() + 1, testCases.size());
    }

    // Rule-REPORT-021
    public void testADeletedTestCaseIsNamedAsTheTestRunNamesIt() {
        final @NotNull TestRunDirectoryDto testRun = theTestRun();
        final @NotNull String named = Bundle.message("testcase.deleted", deleted);

        assertPrints("The web page", html(testRun), named);
        assertPrints("The PDF", pdf(testRun), named);
        assertPrints("The Word document", word(testRun), named);
        assertPrints("The spreadsheet", excel(testRun), named);
    }

    // Rule-REPORT-023
    public void testALineBreakTheTesterTypedStaysALineBreak() {
        final @NotNull TestRunDirectoryDto testRun = theTestRun();

        for (final String report : List.of(pdf(testRun), word(testRun), excel(testRun))) {
            assertTrue("a description was run together onto one line", report.lines().anyMatch(line -> line.strip().endsWith("Open the app")));
            assertTrue("an actual result was run together onto one line", report.lines().anyMatch(line -> line.strip().endsWith("The account")));
        }

        final @NotNull String html = html(testRun);
        assertPrints("The web page", html, "Open the app\nLog in");
        assertTrue("the web page lays a test case's description out on one line", html.matches("(?s).*\\.detail-table td \\{[^}]*white-space: pre-wrap.*"));
        assertTrue("the web page lays an actual result out on one line", html.matches("(?s).*\\.actual \\{[^}]*white-space: pre-wrap.*"));
    }
}
