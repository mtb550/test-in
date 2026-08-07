package org.testin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestinHideActivity implements ProjectActivity {
    @Nullable
    @Override
    public Object execute(final @NotNull Project p, @NotNull Continuation<? super Unit> continuation) {
        ChangeListManager manager = ChangeListManager.getInstance(p);

        LocalChangeList list = manager.findChangeList("testin Automation");
        if (list == null) {
            manager.addChangeList("testin Automation", "Files managed by automation - do not commit");
        }
        return Unit.INSTANCE;
    }
}