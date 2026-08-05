package org.testin.generateReport;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.runEditor.RunEditor;
import org.testin.enums.FileTypes;
import org.testin.mappers.Config;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;
import org.testin.util.KeyboardSet;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GenerateReportAction extends DumbAwareAction {
    private final @Nullable SimpleTree tree;
    private final @Nullable IEditor editor;
    private final @Nullable JBList<TestCaseDto> list;

    public GenerateReportAction(final @NotNull SimpleTree tree) {
        super("Generate Report", "Generate test run report", AllIcons.ToolbarDecorator.Export);
        this.tree = tree;
        this.editor = null;
        this.list = null;
    }

    public GenerateReportAction(final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super("Generate Report", "Generate test run report", null);
        this.tree = null;
        this.editor = editor;
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.GenerateReports.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;
        final Project project = e.getProject();

        TestRunDirectoryDto tr = null;

        if (tree != null) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (selectedNode != null && selectedNode.getUserObject() instanceof TestRunDirectoryDto dto) {
                tr = dto;
            }
        } else if (editor instanceof RunEditor re) {
            tr = re.getParent();
        }

        if (tr == null) return;

        String suggestedName = tr.getPath().getFileName().toString() + "_Report";
        GenerateReportDialog dialog = new GenerateReportDialog(project, suggestedName);
        if (dialog.showAndGet()) {
            processAndSave(project, tr, dialog.getSelectedFormat(), dialog.getSelectedFile());
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (tree != null) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            e.getPresentation().setEnabled(selectedNode != null && selectedNode.getUserObject() instanceof TestRunDirectoryDto);
        } else e.getPresentation().setEnabled(editor instanceof RunEditor);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private void processAndSave(final Project project, final TestRunDirectoryDto tr, final FileTypes format, final File outputFile) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path dirPath = tr.getPath();

                final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);
                TestRunDto runData = indexer.getTestRunByPath(dirPath);

                Map<UUID, TestCaseDto> detailsMap = fetchTestCaseDetails(project, runData);

                byte[] fileBytes = format.generateReport(project, tr, runData, detailsMap);

                File reportFile;
                if (outputFile != null) {
                    reportFile = outputFile;
                } else {
                    String cleanName = runData.getReleaseNotes().replace(".json", "");
                    String rawTimestamp = java.time.ZonedDateTime.now().format(Config.getDateFormatterPattern());
                    String safeTimestamp = rawTimestamp.replace(":", "-").replace("/", "-");
                    reportFile = dirPath.resolve(cleanName + "_Report_" + safeTimestamp + format.getExtension()).toFile();
                }

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

    private Map<UUID, TestCaseDto> fetchTestCaseDetails(final Project project, final TestRunDto tr) {
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

    public @Nullable JBList<TestCaseDto> getList() {
        return list;
    }
}
