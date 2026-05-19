package org.testin.util.reports;

import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.util.Tools;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class HtmlGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
                .append("<th id='col-title'>Title</th>")
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

                String statusText = result.getStatus().name();
                String colorHex = "#" + result.getStatus().getHex();
                String duration = Tools.getInstance().getFormattedDuration(result.getDuration());

                String createdAt = d.getCreatedAt().format(DATE_FORMATTER);
                String updatedAt = d.getUpdatedAt().format(DATE_FORMATTER);

                String groups = d.getGroup().stream().map(Enum::name).collect(Collectors.joining("<br>"));
                String steps = String.join("<br>", d.getSteps());
                String fqcn = String.join("<br>", d.getFqcn());

                html.append("<tr>")
                        .append(cell("col-seq", String.valueOf(seq.getAndIncrement()), "40px"))
                        .append(cell("col-id", id == null ? "" : id.toString(), "250px"))
                        .append(cell("col-title", d.getDescription(), "500px"))
                        .append(statusCell(statusText, colorHex))
                        .append(cell("col-duration", duration, "100px"))
                        .append(cell("col-expected", d.getExpectedResult(), "500px"))
                        .append(cell("col-priority", d.getPriority().name(), "80px"))
                        .append(cell("col-module", d.getModule(), "150px"))
                        .append(cell("col-groups", groups, "150px"))
                        .append(cell("col-created-by", d.getCreatedBy(), "150px"))
                        .append(cell("col-updated-by", d.getUpdatedBy(), "150px"))
                        .append(cell("col-created-at", createdAt, "250px"))
                        .append(cell("col-updated-at", updatedAt, "250px"))
                        .append(cell("col-reference", d.getReference(), "150px"))
                        .append(cell("col-steps", steps, "300px"))
                        .append(cell("col-fqcn", fqcn, "250px"))
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

    private String statusCell(String statusText, String colorHex) {
        return "<td id='col-status' style='color:" + colorHex + "; font-weight:bold;'>" +
                "<div class='cell-content' style='max-width:100px;'>" + statusText + "</div>" +
                "</td>";
    }

    private String cell(String colId, String content, String maxWidth) {
        return "<td id='" + colId + "'>" +
                "<div class='cell-content' style='max-width:" + maxWidth + ";'>" + content + "</div>" +
                "</td>";
    }
}