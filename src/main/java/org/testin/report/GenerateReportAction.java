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
import org.testin.actions.AbstractProjectAction;
import org.testin.editor.TestinEditor;
import org.testin.editor.run.RunEditor;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.importexport.FileTypes;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
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
import java.util.Optional;
import java.util.function.Supplier;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GenerateReportAction extends AbstractProjectAction {

    /**
     * Which run this action reports on, decided by the surface it was built for:
     * the tree's selected node, or the run an editor is showing.
     * <p>
     * It used to be two nullable fields, and every reader worked out again which
     * constructor had been called (#71).
     */
    private final @NotNull Supplier<Optional<TestRunDirectoryDto>> selectedRun;

    /**
     * The tree entry, which registers the shortcut itself: the context menu
     * builds this once with the tree it belongs to, so there is nothing for the
     * tree's own registerShortcuts to add that would not bind the key twice.
     */
    public GenerateReportAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, "Generate Report", "Generate test run report", AllIcons.ToolbarDecorator.Export);
        this.selectedRun = () -> TreeValueUtil.valueOf(tree.getLastSelectedPathComponent(), TestRunDirectoryDto.class);
        registerCustomShortcutSet(Shortcuts.GenerateReport.getCustomShortcut(), tree);
    }

    public GenerateReportAction(final @NotNull Project p, final @NotNull TestinEditor editor) {
        super(p, "Generate Report", "Generate test run report", null);
        this.selectedRun = () -> editor instanceof RunEditor re ? Optional.of(re.getParent()) : Optional.empty();
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
     * True when the current selection resolves to a test run.
     */
    public boolean isAvailable() {
        return selectedRun.get().isPresent();
    }

    /**
     * Direct entry point for toolbar buttons — no AnActionEvent required.
     */
    public void execute() {
        selectedRun.get().ifPresent(tr -> new GenerateReportDialog(p,
                tr.getPath().getFileName().toString() + "_Report",
                (format, file) -> processAndSave(p, tr, format, file)).show());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private void processAndSave(final @NotNull Project p, final @NotNull TestRunDirectoryDto tr,
                                final @NotNull FileTypes format, final @NotNull File outputFile) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final Path dirPath = tr.getPath();

                final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                final TestRunDto runData = indexer.getTestRunByPath(dirPath);

                final Map<UUID, TestCaseDto> detailsMap = fetchTestCaseDetails(p, runData);

                final byte[] fileBytes = format.generateReport(p, tr, runData, detailsMap);

                Files.write(outputFile.toPath(), fileBytes);

                final Notifier notifier = Services.getInstance(p, Notifier.class);
                final NotificationAction openAction = notifier.action("Open report", () -> {

                    try {
                        java.awt.Desktop.getDesktop().open(outputFile);
                    } catch (final Exception openEx) {
                        Logger.error("Failed to open report: " + openEx.getMessage());
                    }
                });

                final NotificationAction copyAction = new NotificationAction("Copy path") {
                    @Override
                    public void actionPerformed(final @NotNull AnActionEvent e, final @NotNull Notification notification) {
                        CopyPasteManager.getInstance().setContents(new StringSelection(outputFile.getAbsolutePath()));
                    }
                };
                copyAction.getTemplatePresentation().setIcon(AllIcons.Actions.Copy);

                Services.getInstance(p, Notifier.class).infoWithActions(p,
                        format.name() + " Report Generated",
                        "Saved successfully: " + outputFile.getName(),
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
            indexer.findTestCase(item.getId()).ifPresent(tc -> detailsMap.put(item.getId(), tc));
        }

        return detailsMap;
    }

}
