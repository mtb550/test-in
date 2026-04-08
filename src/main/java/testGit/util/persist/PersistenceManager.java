package testGit.util.persist;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.Service.Level;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Config;
import testGit.pojo.Groups;
import testGit.pojo.Priority;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.Notifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service(Level.PROJECT)
public final class PersistenceManager implements Disposable {
    public static PersistenceManager getInstance(final Project project) {
        return project.getService(PersistenceManager.class);
    }

    public void updateTitles(final List<TestCaseDto> items, final String[] newTitles, final Runnable onUpdate) {
        int limit = Math.min(newTitles.length, items.size());
        for (int i = 0; i < limit; i++) {
            if (!newTitles[i].trim().isEmpty()) {
                items.get(i).setTitle(newTitles[i].trim());
            }
        }
        // TODO: save to db
        if (onUpdate != null) onUpdate.run();
    }

    public void updateExpected(final List<TestCaseDto> items, final String[] newExpected, final Runnable onUpdate) {
        int limit = Math.min(newExpected.length, items.size());
        for (int i = 0; i < limit; i++) {
            items.get(i).setExpected(newExpected[i].trim());
        }
        // TODO: save to db
        if (onUpdate != null) onUpdate.run();
    }

    public void updatePriority(final List<TestCaseDto> items, final Priority[] newPriorities, final Runnable onUpdate) {
        int limit = Math.min(newPriorities.length, items.size());
        for (int i = 0; i < limit; i++) {
            if (newPriorities[i] != null) {
                items.get(i).setPriority(newPriorities[i]);
            }
        }
        // TODO: save to db
        if (onUpdate != null) onUpdate.run();
    }

    public void updateSteps(final List<TestCaseDto> items, final List<List<String>> newSteps, final Runnable onUpdate) {
        int limit = Math.min(newSteps.size(), items.size());
        for (int i = 0; i < limit; i++) {
            List<String> cleanSteps = newSteps.get(i).stream()
                    .filter(step -> !step.trim().isEmpty())
                    .toList();

            items.get(i).setSteps(cleanSteps);
        }
        // TODO: save to db
        if (onUpdate != null) onUpdate.run();
    }

    public void updateGroups(final List<TestCaseDto> items, final List<List<Groups>> newGroupsList, final Runnable onUpdate) {
        int limit = Math.min(newGroupsList.size(), items.size());
        for (int i = 0; i < limit; i++) {
            List<Groups> groups = newGroupsList.get(i);
            items.get(i).setGroups(groups.isEmpty() ? null : new ArrayList<>(groups));
        }
        // TODO: save to db
        if (onUpdate != null) onUpdate.run();
    }

    public void persistNewTestCase(final Path parentPath, final TestCaseDto newTc, final @Nullable TestCaseDto lastTc) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            try {
                File newJsonFile = parentPath.resolve(newTc.getId() + ".json").toFile();
                Config.getMapper().writeValue(newJsonFile, newTc);

                if (lastTc != null) {
                    File lastJsonFile = parentPath.resolve(lastTc.getId() + ".json").toFile();
                    Config.getMapper().writeValue(lastJsonFile, lastTc);
                }

                Notifier.info("Test Case Created", newTc.getTitle());

            } catch (IOException e) {
                Notifier.error("error", "Failed to create Test case: " + e.getMessage());
            }

        });
    }

    @Override
    public void dispose() {

    }
}