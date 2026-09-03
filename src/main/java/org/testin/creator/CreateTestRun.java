package org.testin.creator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.ExplorerPanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.DirectoryMapper;
import org.testin.model.TestRunConfiguration;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.markers.TestRunMarker;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.RunConfigurationForm;
import org.testin.testrun.RunForm;
import org.testin.testrun.RunFormAction;
import org.testin.ui.framework.SelectionTree;
import org.testin.util.BackgroundWork;
import org.testin.util.EditorUtil;

import java.nio.file.Path;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
public class CreateTestRun implements NodeCreator {
    private final @NotNull Project p;

    /**
     * Asynchronous creator: shows the run configuration dialog and completes on OK,
     * including its own tree refresh and editor opening. Always returns null.
     */
    @Override
    public @NotNull Optional<DirectoryDto> execute(final @NotNull String name, final @NotNull DirectoryDto parentDir, final @NotNull Path newDirPath) {
        // The tree this was started from only exists when a project is bound, so
        // nobody can click their way into the miss. It is checked because a run
        // written against no project would be a directory nothing owns.
        Services.getInstance(p, BoundTestProject.class).get().ifPresentOrElse(
                tp -> configureRun(tp.getTestCasesDirectory(), name, parentDir, Set.of(), Map.of()),
                () -> Logger.warn("Create test run: no test project is bound to " + p.getName()));

        return Optional.empty();
    }

    /**
     * Opens the run form set to create, which is what makes a run and what makes
     * the next cycle: they differ only in what the form opens holding - the
     * previous cycle's cases ticked and its configuration filled in - and not at
     * all in how the run is written, which is what keeps a re-created run from
     * being a second kind of run (#9).
     */
    public void configureRun(final @NotNull DirectoryDto testCasesRoot, final @NotNull String name, final @NotNull DirectoryDto parentDir, final @NotNull Set<UUID> sourceCases, final @NotNull Map<TestRunConfiguration, String> sourceConfiguration) {
        new RunForm(p).open(testCasesRoot, name, sourceCases, sourceConfiguration,
                new RunFormAction("Create Test Run", "Create", (form, selection) -> create(form, selection, parentDir)));
    }

    /**
     * Writes the run, or refuses and says why - and answers which, because the
     * dialog stays open on a refusal.
     * <p>
     * The name is resolved here rather than before the dialog opened, since here
     * is where the tester finished deciding it. That also puts the two checks
     * the name needs in one place: the tree's create action makes them for the
     * name it asks for, and nothing made them for a name typed afterwards.
     */
    private boolean create(final @NotNull RunConfigurationForm form, final @NotNull SelectionTree selection, final @NotNull DirectoryDto parentDir) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);

        final @NotNull String name = form.getRunName();
        if (name.isEmpty()) {
            notifier.softRefuse(p, "A test run needs a name");
            return false;
        }

        // The popup is not modal - the tree stays live while the dialog is open,
        // so the parent may have been removed.
        if (!indexer.nodeExists(parentDir.getPath())) {
            notifier.softRefuse(p, "'" + parentDir.getName() + "' no longer exists - test run not created");
            return false;
        }

        final @NotNull Path savePath = parentDir.getPath().resolve(name);
        if (indexer.nodeExists(savePath)) {
            notifier.softShowExists(p, name);
            return false;
        }

        final @NotNull TestRunDirectoryDto tr = Services.getInstance(p, DirectoryMapper.class).setTestRunNode(p, savePath, parentDir);
        saveSelectedToJSON(form, selection, savePath, Services.getInstance(p, ExplorerPanel.class), tr);

        return true;
    }



    private void saveSelectedToJSON(final @NotNull RunConfigurationForm form, final @NotNull SelectionTree selection, final @NotNull Path savePath, final @NotNull ExplorerPanel pp, final @NotNull TestRunDirectoryDto trDir) {
        // Read once, here, while the dialog is still on screen. Everything below
        // works from this map rather than going back to the form, and the
        // background write further down could not go back to it anyway (#87).
        final @NotNull Map<TestRunConfiguration, String> configuration = form.configuration();

        // Who made it and when is the marker's, which every node carries and the
        // details popup already reads. The run held a second copy that nothing
        // ever read back.
        final @NotNull TestRunDto tr = new TestRunDto()
                .setConfiguration(TestRunConfiguration.answered(configuration));

        final @NotNull List<TestRunItems> items = new ArrayList<>();
        RunForm.checkedCases(selection).forEach(id -> items.add(new TestRunItems().setId(id).setStatus(TestStatus.PENDING)));
        tr.setResults(items);

        // The form and the checked tree were read above, while the dialog was
        // still there; from here nothing touches a component (#87).
        BackgroundWork.run(p, "Creating test run " + savePath.getFileName(), "Test Run Not Created", indicator -> {
            Services.getInstance(p, ProjectIndexer.class).putTestRun(savePath, tr);

            // Defaults are correct (status CREATED); addTestRunDir stamps the
            // tester's audit info before the marker's first write.
            final @NotNull TestRunMarker marker = new TestRunMarker();
            trDir.setMarker(marker);

            Services.getInstance(p, ProjectIndexer.class).addTestRunDir(trDir);
            Services.getInstance(p, ProjectIndexer.class).updateRunMarker(p, savePath, marker);

            // File access is the indexer's alone (see CLAUDE.md).
            Services.getInstance(p, ProjectIndexer.class).refreshDirectory(savePath);

            ApplicationManager.getApplication().invokeLater(() -> {
                pp.getProjectTree().refresh();
                Services.getInstance(p, EditorUtil.class).open(p, trDir);

                // Here rather than in CreateTreeNodeAction: creating a run is
                // asynchronous, and the action returns while the dialog is still
                // open (#62).
                Services.getInstance(p, Notifier.class).softShow(p, "Run created");
            });

        });
    }


}

