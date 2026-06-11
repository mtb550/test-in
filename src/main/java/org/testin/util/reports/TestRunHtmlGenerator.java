package org.testin.util.reports;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.RunEditorAttributes;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.TestStatus;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.util.Tools;
import org.testin.util.services.Services;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class TestRunHtmlGenerator {

    public String generate(final @NotNull Project project, final @NotNull TestRunDirectoryDto trdir, final @NotNull TestRunDto tr, final Map<UUID, TestCaseDto> detailsMap) {
        StringBuilder html = new StringBuilder();

        html.append("<html><head><style>")
                .append(".table-container { width: 100%; overflow-x: auto; border: 1px solid #ccc; margin-top: 10px; }")
                .append("table { border-collapse: collapse; font-size: 12px; font-family: sans-serif; width: max-content; }")
                .append("th { background-color: #f4f4f4; text-align: left; padding: 8px; border: 1px solid #ddd; }")
                .append("td { padding: 0; border: 1px solid #ddd; vertical-align: top; }")
                .append(".cell-content { padding: 8px; overflow-wrap: break-word; white-space: normal; }")
                .append("</style></head><body>");

        html.append("<h2>Test Run Report: ").append(tr.getRunName().replace(".json", "")).append("</h2>");
        html.append("<p><strong>Platform:</strong> ").append(tr.getPlatform()).append("</p>");
        html.append("<p><strong>Status:</strong> ").append(trdir.getMarker().getStatus().name()).append("</p>");

        html.append("<div class='table-container'><table><tr>")
                .append("<th class='col-seq'>#</th>")
                .append(String.format("<th class='col-id'>%s</th>", TestEditorAttributes.ID.getName()))
                .append(String.format("<th class='col-description'>%s</th>", TestEditorAttributes.DESCRIPTION.getName()))
                .append(String.format("<th class='col-status'>%s</th>", RunEditorAttributes.RUN_STATUS.getName()))
                .append(String.format("<th class='col-duration'>%s</th>", RunEditorAttributes.DURATION.getName()))
                .append(String.format("<th class='col-expected'>%s</th>", TestEditorAttributes.EXPECTED_RESULT.getName()))
                .append(String.format("<th class='col-priority'>%s</th>", TestEditorAttributes.PRIORITY.getName()))
                .append(String.format("<th class='col-module'>%s</th>", TestEditorAttributes.MODULE.getName()))
                .append(String.format("<th class='col-groups'>%s</th>", TestEditorAttributes.GROUP.getName()))
                .append(String.format("<th class='col-created-by'>%s</th>", TestEditorAttributes.CREATE_BY.getName()))
                .append(String.format("<th class='col-updated-by'>%s</th>", TestEditorAttributes.UPDATE_BY.getName()))
                .append(String.format("<th class='col-created-at'>%s</th>", TestEditorAttributes.CREATE_AT.getName()))
                .append(String.format("<th class='col-updated-at'>%s</th>", TestEditorAttributes.UPDATE_AT.getName()))
                .append(String.format("<th class='col-reference'>%s</th>", TestEditorAttributes.REFERENCE.getName()))
                .append(String.format("<th class='col-steps'>%s</th>", TestEditorAttributes.STEPS.getName()))
                .append(String.format("<th class='col-fqcn'>%s</th>", TestEditorAttributes.FQCN.getName()))
                .append("<th class='col-code'>Code</th>")
                .append("</tr>");

        if (!tr.getResults().isEmpty()) {
            AtomicInteger seq = new AtomicInteger(1);

            tr.getResults().forEach(result -> {
                UUID id = result.getId();
                TestCaseDto d = detailsMap.get(id);

                html.append("<tr>")
                        .append(cell("col-seq", String.valueOf(seq.getAndIncrement()), "40px"))
                        .append(cell("col-id", id.toString(), "250px"))
                        .append(descriptionCell(d.getDescription()))
                        .append(statusCell(result.getStatus()))
                        .append(durationCell(project, result.getDuration()))
                        .append(cell("col-expected", d.getExpectedResult(), "500px"))
                        .append(cell("col-priority", d.getPriority().name(), "80px"))
                        .append(cell("col-module", d.getModule(), "150px"))
                        .append(groupsCell(d.getGroup()))
                        .append(cell("col-created-by", d.getCreatedBy(), "150px"))
                        .append(cell("col-updated-by", d.getUpdatedBy(), "150px"))
                        .append(dateCell("col-created-at", d.getCreatedAt()))
                        .append(dateCell("col-updated-at", d.getUpdatedAt()))
                        .append(cell("col-reference", d.getReference(), "150px"))
                        .append(stepsCell(d.getSteps()))
                        .append(fqcnCell(d.getFqcn()))
                        .append(cell("col-code", "<a href='#'>Navigate</a>", "80px"))
                        .append("</tr>");
            });
        } else {
            html.append("<tr><td colspan='17' style='text-align:center;'>No test results found.</td></tr>");
        }

        html.append("</table>");
        html.append("</div>");

        html.append("<div style='margin-top: 25px; text-align: center; font-size: 11px; color: #888;'>")
                .append("<p>This test run report was automatically generated by the <a href='https://plugins.jetbrains.com/plugin/31514-testin' target='_blank' style='color: #0052cc; text-decoration: none;'><strong>Testin</strong></a> IntelliJ plugin.</p>")
                .append("</div>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private String descriptionCell(final String description) {
        String content = description != null ? description : "";
        return cell("col-description", "<b>" + content + "</b>", "500px");
    }

    private String statusCell(final TestStatus status) {
        if (status == null) return cell("col-status", "", "100px");

        String colorHex = "#" + status.getHex();
        return "<td class='col-status' style='color:" + colorHex + "; font-weight:bold;'>" +
                "<div class='cell-content' style='max-width:100px;'>" + status.name() + "</div>" +
                "</td>";
    }

    private String durationCell(final @NotNull Project project, final Duration duration) {
        String formatted = Services.getInstance(project, Tools.class).getFormattedDuration(duration);
        return cell("col-duration", formatted, "100px");
    }

    private String dateCell(final String colClass, final ZonedDateTime dateTime) {
        String formatted = dateTime != null ? dateTime.format(Config.getDateFormatterPattern()) : "";
        return cell(colClass, formatted, "250px");
    }

    private String groupsCell(final List<? extends Enum<?>> groups) {
        String content = groups != null ? groups.stream().map(Enum::name).collect(Collectors.joining("<br>")) : "";
        return cell("col-groups", content, "150px");
    }

    private String stepsCell(final List<String> steps) {
        String content = steps != null ? String.join("<br>", steps) : "";
        return cell("col-steps", content, "300px");
    }

    private String fqcnCell(final List<String> fqcn) {
        String content = fqcn != null ? String.join("<br>", fqcn) : "";
        return cell("col-fqcn", content, "250px");
    }

    private String cell(final String colClass, final String content, final String maxWidth) {
        return "<td class='" + colClass + "'>" +
                "<div class='cell-content' style='max-width:" + maxWidth + ";'>" + content + "</div>" +
                "</td>";
    }
}