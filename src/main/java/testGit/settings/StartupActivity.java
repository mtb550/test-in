package testGit.settings;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.util.Runner.TestCaseExecutionTracker;

import java.nio.file.Path;
import java.util.Optional;

public class StartupActivity {
    public static void execute(@NotNull Project project) {
        System.out.println("StartupActivity.execute()");

        AppSettingsState settings = AppSettingsState.getInstance();

        Path testGitPath;
        if (settings.rootTestGitPath != null && !settings.rootTestGitPath.isEmpty()) {
            testGitPath = Path.of(settings.rootTestGitPath);
        } else {
            testGitPath = Optional.ofNullable(project.getBasePath())
                    .map(Path::of)
                    .map(p -> p.resolve("testGit"))
                    .orElse(null);
        }

        Path automationPath = null;
        if (settings.rootAutomationPath != null && !settings.rootAutomationPath.isEmpty()) {
            String folderFormat = settings.rootAutomationPath.replace(".", "/");

            automationPath = Optional.ofNullable(project.getBasePath())
                    .map(Path::of)
                    .map(p -> p.resolve(folderFormat))
                    .orElse(null);
        }

        System.out.println("testGit Path: " + testGitPath);
        System.out.println("automation Path: " + automationPath);

        Config.setTestGitPath(testGitPath);
        Config.setAutomationPath(automationPath);

        /// to be removed
        Config.setProject(project);

        TestCaseExecutionTracker.initGlobalListener(Config.getProject());
    }
}