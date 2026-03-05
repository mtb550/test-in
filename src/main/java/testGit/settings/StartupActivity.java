package testGit.settings;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;

import java.nio.file.Path;
import java.util.Optional;

public class StartupActivity {
    public static void execute(@NotNull Project project) {
        System.out.println("StartupActivity.execute()");

        Path testGitPath = Optional.ofNullable(project.getBasePath())
                .map(Path::of)
                .map(p -> p.resolve("testGit"))
                .orElse(null);

        System.out.println("testGit Path" + testGitPath);

        Config.setTestGitPath(testGitPath);
        Config.setProject(project);
    }
}