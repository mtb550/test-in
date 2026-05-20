package org.testin.util.reports;

import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.TestStatus;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.util.Tools;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class HtmlGenerator {

    public String generate(final @NotNull TestRunDto tr, final Map<UUID, TestCaseDto> detailsMap) {
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
        html.append("<p><strong>Status:</strong> ").append(tr.getStatus().name()).append("</p>");

        html.append("<div class='table-container'><table><tr>")
                .append("<th id='col-seq'>#</th>")
                .append("<th id='col-id'>ID</th>")
                .append("<th id='col-description'>Description</th>")
                .append("<th id='col-status'>Status</th>")
                .append("<th id='col-duration'>Duration</th>")
                .append("<th id='col-expected'>Expected Result</th>")
                .append("<th id='col-priority'>Priority</th>")
                .append("<th id='col-module'>Module</th>")
                .append("<th id='col-groups'>Groups</th>")
                .append("<th id='col-created-by'>Created By</th>")
                .append("<th id='col-updated-by'>Updated By</th>")
                .append("<th id='col-created-at'>Created At</th>")
                .append("<th id='col-updated-at'>Updated At</th>")
                .append("<th id='col-reference'>Reference</th>")
                .append("<th id='col-steps'>Steps</th>")
                .append("<th id='col-fqcn'>FQCN</th>")
                .append("<th id='col-code'>Code</th>")
                .append("</tr>");

        if (!tr.getResults().isEmpty()) {
            AtomicInteger seq = new AtomicInteger(1);

            tr.getResults().forEach(result -> {
                UUID id = result.getTestCaseId();
                TestCaseDto d = detailsMap.get(id);

                html.append("<tr>")
                        .append(cell("col-seq", String.valueOf(seq.getAndIncrement()), "40px"))
                        .append(cell("col-id", id == null ? "" : id.toString(), "250px"))
                        .append(descriptionCell(d.getDescription()))
                        .append(statusCell(result.getStatus()))
                        .append(durationCell(result.getDuration()))
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
        return "<td id='col-status' style='color:" + colorHex + "; font-weight:bold;'>" +
                "<div class='cell-content' style='max-width:100px;'>" + status.name() + "</div>" +
                "</td>";
    }

    private String durationCell(final Duration duration) {
        String formatted = Tools.getInstance().getFormattedDuration(duration);
        return cell("col-duration", formatted, "100px");
    }

    private String dateCell(final String colId, final ZonedDateTime dateTime) {
        String formatted = dateTime != null ? dateTime.format(Config.getDateFormatterPattern()) : "";
        return cell(colId, formatted, "250px");
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

    private String cell(final String colId, final String content, final String maxWidth) {
        return "<td id='" + colId + "'>" +
                "<div class='cell-content' style='max-width:" + maxWidth + ";'>" + content + "</div>" +
                "</td>";
    }
}