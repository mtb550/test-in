package org.testin.report;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.AbstractProjectAction;
import org.testin.editor.TestinEditor;
import org.testin.editor.run.RunEditor;
import org.testin.enums.FileTypes;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.Config;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Tools;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GenerateReportAction extends AbstractProjectAction {

    private static final @NotNull KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK);
    private final @Nullable SimpleTree tree;
    private final @Nullable TestinEditor editor;

    public GenerateReportAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, "Generate Report", "Generate test run report", AllIcons.ToolbarDecorator.Export);
        this.tree = tree;
        this.editor = null;
    }

    public GenerateReportAction(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Generate Report", "Generate test run report", null);
        this.tree = null;
        this.editor = editor;
        this.registerCustomShortcutSet(Tools.customShortcut(SHORTCUT), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(isAvailable());
    }

    /**
     * True when the current selection resolves to a test run.
     */
    public boolean isAvailable() {
        if (tree != null) {
            return TreeValueUtil.valueOf(tree.getLastSelectedPathComponent(), TestRunDirectoryDto.class) != null;
        }
        return editor instanceof RunEditor;
    }

    /**
     * Direct entry point for toolbar buttons — no AnActionEvent required.
     */
    public void execute() {

        TestRunDirectoryDto tr = null;

        if (tree != null) {
            tr = TreeValueUtil.valueOf(tree.getLastSelectedPathComponent(), TestRunDirectoryDto.class);
        } else if (editor instanceof RunEditor re) {
            tr = re.getParent();
        }

        if (tr == null) return;

        final String suggestedName = tr.getPath().getFileName().toString() + "_Report";

        // The callback needs an effectively final reference; the run is assigned in a branch above.
        final TestRunDirectoryDto runDir = tr;
        new GenerateReportDialog(p, suggestedName, (format, file) -> processAndSave(p, runDir, format, file)).show();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private void processAndSave(final @NotNull Project p, final @NotNull TestRunDirectoryDto tr,
                                final @NotNull FileTypes format, final @Nullable File outputFile) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final Path dirPath = tr.getPath();

                final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                final TestRunDto runData = indexer.getTestRunByPath(dirPath);
                if (runData == null) {
                    Services.getInstance(p, Notifier.class).error(p, "Report Error",
                            "No test run data found at: " + dirPath);
                    return;
                }

                final Map<UUID, TestCaseDto> detailsMap = fetchTestCaseDetails(p, runData);

                final byte[] fileBytes = format.generateReport(p, tr, runData, detailsMap);

                final File reportFile;
                if (outputFile != null) {
                    reportFile = outputFile;
                } else {
                    final String cleanName = runData.getChangeLog().replace(".json", "");
                    final String rawTimestamp = java.time.ZonedDateTime.now().format(Config.getDateFormatterPattern());
                    final String safeTimestamp = rawTimestamp.replace(":", "-").replace("/", "-");
                    reportFile = dirPath.resolve(cleanName + "_Report_" + safeTimestamp + format.getExtension()).toFile();
                }

                Files.write(reportFile.toPath(), fileBytes);

                final NotificationAction openAction = NotificationAction.createSimple("Open report", () -> {

                    try {
                        java.awt.Desktop.getDesktop().open(reportFile);
                    } catch (final Exception openEx) {
                        Logger.error("Failed to open report: " + openEx.getMessage());
                    }
                });

                final NotificationAction copyAction = new NotificationAction("Copy path") {
                    @Override
                    public void actionPerformed(final @NotNull AnActionEvent e, final @NotNull Notification notification) {
                        CopyPasteManager.getInstance().setContents(new StringSelection(reportFile.getAbsolutePath()));
                    }
                };
                copyAction.getTemplatePresentation().setIcon(AllIcons.Actions.Copy);

                Services.getInstance(p, Notifier.class).infoWithActions(p,
                        format.name() + " Report Generated",
                        "Saved successfully: " + reportFile.getName(),
                        openAction,
                        copyAction
                );

            } catch (final Exception ex) {
                Services.getInstance(p, Notifier.class).error(p, "Report Error", "Failed to generate " + format.name() + " report: " + ex.getMessage());
                Logger.error("Exception: " + ex.getMessage());
            }
        });
    }

    private @NotNull Map<UUID, TestCaseDto> fetchTestCaseDetails(final @NotNull Project p, final @NotNull TestRunDto tr) {
        final Map<UUID, TestCaseDto> detailsMap = new ConcurrentHashMap<>();

        if (tr.getResults().isEmpty()) {
            return detailsMap;
        }

        final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        for (final TestRunItems item : tr.getResults()) {
            final TestCaseDto tc = indexer.getTestCaseById(item.getId());
            if (tc != null) {
                detailsMap.put(item.getId(), tc);
            }
        }

        return detailsMap;
    }

}
