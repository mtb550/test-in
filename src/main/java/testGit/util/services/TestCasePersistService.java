package testGit.util.services;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.Service.Level;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.notifications.Notifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service(Level.PROJECT)
public final class TestCasePersistService implements Disposable {
    public static TestCasePersistService getInstance(final Project project) {
        return project.getService(TestCasePersistService.class);
    }

    /**
     * @param path test set path
     * @param tcs  list of changed or created test cases
     */
    public void persist(final Path path, final @Nullable List<TestCaseDto> tcs) {
        if (path == null || tcs == null || tcs.isEmpty()) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            tcs.stream()
                    .filter(tc -> tc != null && tc.getId() != null)
                    .forEach(tc -> {
                        try {
                            File jsonFile = path.resolve(tc.getId() + ".json").toFile();
                            Config.getMapper().writeValue(jsonFile, tc);
                        } catch (IOException e) {
                            Notifier.error("Save Error", "Failed to persist data: " + e.getMessage());
                        }
                    });
            Notifier.info("Test Case Created", tcs.getFirst().getTitle());
        });
    }

    @Override
    public void dispose() {
        /// to be implemented
    }
}