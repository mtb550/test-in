package org.testin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

public class TestinCheckinHandlerFactory extends CheckinHandlerFactory {
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
                            return path.contains(Bundle.getPluginName());
                        });

                if (containsManagedFiles) {
                    Project project = panel.getProject();
                    Services.getInstance(project, Notifier.class).error(project, "Commits to 'testin' paths are disabled. These are managed by automation.", "Commit Blocked");
                    return ReturnResult.CANCEL;
                }

                return ReturnResult.COMMIT;
            }
        };
    }
}