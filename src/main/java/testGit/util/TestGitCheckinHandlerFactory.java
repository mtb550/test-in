package testGit.util;

import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;

public class TestGitCheckinHandlerFactory extends CheckinHandlerFactory {
    @Override
    public @NotNull CheckinHandler createHandler(@NotNull CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
        return new CheckinHandler() {
            @Override
            public ReturnResult beforeCheckin() {
                boolean containsManagedFiles = panel.getSelectedChanges().stream()
                        .anyMatch(change -> {
                            String path = change.getAfterRevision() != null
                                    ? change.getAfterRevision().getFile().getPath()
                                    : "";
                            return path.contains("TestGit");
                        });

                if (containsManagedFiles) {
                    Messages.showErrorDialog(
                            panel.getProject(),
                            "Commits to 'TestGit' paths are disabled. These are managed by automation.",
                            "Commit Blocked"
                    );
                    return ReturnResult.CANCEL;
                }

                return ReturnResult.COMMIT;
            }
        };
    }
}