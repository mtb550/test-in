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
                // Get all files currently selected for commit
                boolean containsManagedFiles = panel.getSelectedChanges().stream()
                        .anyMatch(change -> {
                            String path = change.getAfterRevision() != null
                                    ? change.getAfterRevision().getFile().getPath()
                                    : "";
                            return path.contains("TestGit");
                        });

                if (containsManagedFiles) {
                    // This creates the "Disable" effect by blocking the action
                    Messages.showErrorDialog(
                            panel.getProject(),
                            "Commits to 'TestGit' paths are disabled. These are managed by automation.",
                            "Commit Blocked"
                    );
                    return ReturnResult.CANCEL; // Stops the commit process
                }

                return ReturnResult.COMMIT; // Proceeds as normal
            }
        };
    }
}

//implement WritingAccessProvider
/*
That’s a win! It’s a satisfying feeling when the IDE actually listens to your rules and slams the door on an unauthorized commit.

Now that you’ve successfully labeled the changelist and blocked the commit, you have a solid "soft" lock. But if you want to go full "Managed by Automation" mode and prevent users from even typing in those files, there is one final level to this boss fight.

The "Full Lockdown" Workflow
Right now, a developer can still change the code, save it, and only find out they’re blocked when they try to commit. To make it a seamless experience, you can make the files Read-Only in the editor.

Decorator: Tells them why it's special.

CheckinHandler: Prevents the code from reaching the repo.

WritingAccessProvider (Optional): Prevents the code from being changed locally.

Pro-Tip: Improving the Decorator UI
Since you mentioned the path match works, you can make the warning pop a bit more by using specific colors or bold text so it doesn't just blend into the grayed-out defaults:

Java
renderer.append(" [LOCKED BY AUTOMATION]",
    new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBColor.RED));
Would you like the code for the WritingAccessProvider to make those "TestGit" paths read-only, or are you happy with just the commit block for now?
*/