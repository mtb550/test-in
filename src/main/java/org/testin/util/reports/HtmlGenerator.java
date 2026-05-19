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
                .append("table {width:100%; border-collapse:collapse; font-size: 12px; font-family: sans-serif;}")
                .append("th,td {border:1px solid #ddd; padding:6px; vertical-align:top;}")
                .append("th {background-color: #f4f4f4; text-align: left;}")
                .append("</style></head><body>");

        html.append("<h2>Test Run Report: ").append(tr.getRunName().replace(".json", "")).append("</h2>");
        html.append("<p><strong>Platform:</strong> ").append(tr.getPlatform()).append("</p>");
        html.append("<p><strong>Status:</strong> ").append(tr.getStatus().name()).append("</p>");

        html.append("<table><tr>")
                .append("<th>#</th><th>ID</th><th>Title</th><th>Status</th><th>Duration</th>")
                .append("<th>Expected Result</th><th>Priority</th><th>Module</th><th>Groups</th>")
                .append("<th>Created By</th><th>Updated By</th><th>Created At</th><th>Updated At</th>")
                .append("<th>Reference</th><th>Steps</th><th>FQCN</th>")
                .append("</tr>");

        if (!tr.getResults().isEmpty()) {
            AtomicInteger seq = new AtomicInteger(1);

            tr.getResults().forEach(result -> {
                UUID id = result.getTestCaseId();
                TestCaseDto d = (detailsMap != null) ? detailsMap.get(id) : null;

                String statusText = result.getStatus().name();
                String colorHex = "#" + result.getStatus().getHex();
                String duration = Tools.getInstance().getFormattedDuration(result.getDuration());

                String title = d != null ? d.getDescription() : "N/A";
                String expected = d != null ? d.getExpectedResult() : "N/A";
                String priority = d != null ? d.getPriority().name() : "N/A";
                String module = d != null ? d.getModule() : "N/A";
                String createdBy = d != null ? d.getCreatedBy() : "N/A";
                String updatedBy = d != null ? d.getUpdatedBy() : "N/A";
                String reference = d != null ? d.getReference() : "N/A";

                String createdAt = d != null ? d.getCreatedAt().format(DATE_FORMATTER) : "N/A";
                String updatedAt = d != null ? d.getUpdatedAt().format(DATE_FORMATTER) : "N/A";

                String groups = (d != null && !d.getGroup().isEmpty()) ? d.getGroup().stream().map(Enum::name).collect(Collectors.joining("<br>")) : "N/A";
                String steps = (d != null && !d.getSteps().isEmpty()) ? String.join("<br>", d.getSteps()) : "N/A";
                String fqcn = (d != null && !d.getFqcn().isEmpty()) ? String.join("<br>", d.getFqcn()) : "N/A";

                html.append("<tr>")
                        .append("<td>").append(seq.getAndIncrement()).append("</td>")
                        .append("<td>").append(id != null ? id.toString() : "N/A").append("</td>")
                        .append("<td>").append(title).append("</td>")
                        .append("<td style='color:").append(colorHex).append("; font-weight:bold;'>").append(statusText).append("</td>")
                        .append("<td>").append(duration != null ? duration : "N/A").append("</td>")
                        .append("<td>").append(expected).append("</td>")
                        .append("<td>").append(priority).append("</td>")
                        .append("<td>").append(module).append("</td>")
                        .append("<td>").append(groups).append("</td>")
                        .append("<td>").append(createdBy).append("</td>")
                        .append("<td>").append(updatedBy).append("</td>")
                        .append("<td>").append(createdAt).append("</td>")
                        .append("<td>").append(updatedAt).append("</td>")
                        .append("<td>").append(reference).append("</td>")
                        .append("<td>").append(steps).append("</td>")
                        .append("<td>").append(fqcn).append("</td>")
                        .append("</tr>");
            });
        } else {
            html.append("<tr><td colspan='16' style='text-align:center;'>No test results found.</td></tr>");
        }

        html.append("</table></body></html>");
        return html.toString();
    }
}