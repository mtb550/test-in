package org.testin.actions.export;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ExportHtml extends ExportBase {

    public ExportHtml(final @NotNull SimpleTree tree) {
        super(tree, "Export to HTML", "Export test cases to an HTML file", AllIcons.FileTypes.Html);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;
        final Project project = e.getProject();
        final TreePath path = tree.getSelectionPath();

        if (path == null) {
            Services.getInstance(project, Notifier.class).error(project, "Export Error", "Please select a directory in the Project Panel tree.");
            return;
        }

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof DirectoryDto dirDto) ||
                !(dirDto instanceof TestSetDirectoryDto || dirDto instanceof TestSetPackageDirectoryDto || dirDto instanceof TestCasesMainDirectoryDto)) {
            Services.getInstance(project, Notifier.class).error(project, "Export Error", "Please select a valid Test Set, Test Set Package, or Test Cases Directory.");
            return;
        }

        VirtualFile targetDirectory = LocalFileSystem.getInstance().findFileByPath(dirDto.getPath().toString());

        if (targetDirectory != null && !targetDirectory.isDirectory()) {
            targetDirectory = targetDirectory.getParent();
        }

        if (targetDirectory == null) {
            Services.getInstance(project, Notifier.class).error(project, "Export Error", "The selected path in the Project Panel is invalid.");
            return;
        }

        FileSaverDescriptor descriptor = new FileSaverDescriptor("Export HTML", "Save test cases as an HTML file", "html");
        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project);

        String defaultFileName = targetDirectory.getName() + "_Export.html";
        VirtualFileWrapper wrapper = dialog.save((VirtualFile) null, defaultFileName);

        if (wrapper != null) {
            File destFile = wrapper.getFile();
            processExport(project, destFile, targetDirectory, dirDto);
        }
    }

    private void processExport(final @NotNull Project project, final File destFile, final VirtualFile targetDirectory, final DirectoryDto selectedDirDto) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Exporting test cases to HTML", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Gathering test cases...");

                Map<String, List<TestCaseDto>> sheetsData = gatherData(project, targetDirectory, selectedDirDto);

                if (indicator.isCanceled()) return;

                if (sheetsData.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).warn(project, "Export Empty", "No valid test cases found to export in the selected directory."));
                    return;
                }

                indicator.setText("Generating HTML file...");

                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile)))) {
                    writeHtmlDocument(writer, project, sheetsData);

                    NotificationAction openAction = NotificationAction.createSimple("Open file", () ->
                            BrowserUtil.browse(destFile.toURI().toString())
                    );
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).infoWithActions(project,
                                    "Export Complete",
                                    "Successfully exported test cases to:\n" + destFile.getName(),
                                    openAction));

                } catch (final Exception ex) {
                    Log.error("Export crashed: " + ex.getMessage());
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(project, Notifier.class).error(project, "Export Failed",
                                    "Failed to save the HTML file:\n" + ex.getMessage()));
                }
            }
        });
    }

    private void writeHtmlDocument(final @NotNull BufferedWriter writer, final @NotNull Project project,
                                   final Map<String, List<TestCaseDto>> sheetsData) throws IOException {
        writer.write("<!DOCTYPE html>");
        writer.newLine();
        writer.write("<html lang=\"en\">");
        writer.newLine();
        writer.write("<head>");
        writer.newLine();
        writer.write("<meta charset=\"UTF-8\">");
        writer.newLine();
        writer.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        writer.newLine();
        writer.write("<title>Test Cases Export</title>");
        writer.newLine();
        writer.write("<style>");
        writer.newLine();
        writer.write("  body { font-family: Arial, sans-serif; margin: 20px; }");
        writer.newLine();
        writer.write("  h1 { color: #333; }");
        writer.newLine();
        writer.write("  h2 { color: #555; margin-top: 30px; }");
        writer.newLine();
        writer.write("  table { border-collapse: collapse; width: 100%; margin-bottom: 30px; }");
        writer.newLine();
        writer.write("  th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; vertical-align: top; }");
        writer.newLine();
        writer.write("  th { background-color: #f4f4f4; font-weight: bold; }");
        writer.newLine();
        writer.write("  tr:nth-child(even) { background-color: #f9f9f9; }");
        writer.newLine();
        writer.write("  .section-title { margin-top: 20px; }");
        writer.newLine();
        writer.write("</style>");
        writer.newLine();
        writer.write("</head>");
        writer.newLine();
        writer.write("<body>");
        writer.newLine();

        writer.write("<h1>Test Cases Export</h1>");
        writer.newLine();

        int totalExported = 0;

        for (Map.Entry<String, List<TestCaseDto>> entry : sheetsData.entrySet()) {
            final String sheetName = entry.getKey();
            final List<TestCaseDto> testCases = entry.getValue();

            if (testCases.isEmpty()) continue;

            writer.write("<h2>" + htmlEscape(sheetName) + "</h2>");
            writer.newLine();

            writer.write("<table>");
            writer.newLine();

            writer.write("<tr>");
            for (TestEditorAttributes attr : EXPORT_COLUMNS) {
                writer.write("<th>" + htmlEscape(attr.getName()) + "</th>");
            }
            writer.write("</tr>");
            writer.newLine();

            // Data rows
            for (TestCaseDto tc : testCases) {
                writer.write("<tr>");
                for (TestEditorAttributes attr : EXPORT_COLUMNS) {
                    String val = attr.getValueExtractor().apply(tc, project);
                    writer.write("<td>" + htmlEscape(val != null ? val : "") + "</td>");
                }
                writer.write("</tr>");
                writer.newLine();
                totalExported++;
            }

            writer.write("</table>");
            writer.newLine();
        }

        writer.write("<p><em>Total test cases exported: " + totalExported + "</em></p>");
        writer.newLine();

        String exportDate = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss zzz"));
        writer.write("<p><em>Exported on: " + htmlEscape(exportDate) + "</em></p>");
        writer.newLine();

        writer.write("</body>");
        writer.newLine();
        writer.write("</html>");
        writer.newLine();
    }

    private String htmlEscape(final String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();
        final int selectionCount = tree.getSelectionCount();

        if (selectionCount != 1 || path == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = selectedNode.getUserObject();

        e.getPresentation().setEnabled(userObject instanceof TestSetDirectoryDto ||
                userObject instanceof TestSetPackageDirectoryDto ||
                userObject instanceof TestCasesMainDirectoryDto);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
