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
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.importexport.FileTypes;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.Config;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Shortcuts;

import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GenerateReportAction extends AbstractProjectAction {

    private final @Nullable SimpleTree tree;
    private final @Nullable TestinEditor editor;

    /**
     * The tree entry, which registers the shortcut itself: the context menu
     * builds this once with the tree it belongs to, so there is nothing for the
     * tree's own registerShortcuts to add that would not bind the key twice.
     */
    public GenerateReportAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, "Generate Report", "Generate test run report", AllIcons.ToolbarDecorator.Export);
        this.tree = tree;
        this.editor = null;
        registerCustomShortcutSet(Shortcuts.GenerateReport.getCustomShortcut(), tree);
    }

    public GenerateReportAction(final @NotNull Project p, final @NotNull TestinEditor editor) {
        super(p, "Generate Report", "Generate test run report", null);
        this.tree = null;
        this.editor = editor;
    }

    /**
     * The keyboard route, registered on the list it is reached from - the same
     * shape as {@link org.testin.EscapeAction}. The two-argument constructor is
     * the toolbar button, which is clicked rather than typed and so registers
     * nothing.
     */
    public GenerateReportAction(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list) {
        this(p, editor);
        registerCustomShortcutSet(Shortcuts.GenerateReport.getCustomShortcut(), list);
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
     * The run this action reports on: the tree's selected node, or the run the
     * editor is showing. Asked by both {@link #isAvailable()} and
     * {@link #execute()}, so the two can never disagree about which run is meant.
     */
    private @Nullable TestRunDirectoryDto selectedRun() {
        if (tree != null) return TreeValueUtil.valueOf(tree.getLastSelectedPathComponent(), TestRunDirectoryDto.class);
        return editor instanceof RunEditor re ? re.getParent() : null;
    }

    /**
     * True when the current selection resolves to a test run.
     */
    public boolean isAvailable() {
        return selectedRun() != null;
    }

    /**
     * Direct entry point for toolbar buttons — no AnActionEvent required.
     */
    public void execute() {
        final TestRunDirectoryDto tr = selectedRun();
        if (tr == null) return;

        final String suggestedName = tr.getPath().getFileName().toString() + "_Report";

        new GenerateReportDialog(p, suggestedName, (format, file) -> processAndSave(p, tr, format, file)).show();
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

                final Notifier notifier = Services.getInstance(p, Notifier.class);
                final NotificationAction openAction = notifier.action("Open report", () -> {

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
