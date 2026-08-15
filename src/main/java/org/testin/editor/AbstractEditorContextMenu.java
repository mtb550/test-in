package org.testin.editor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.components.JBList;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.dto.TestCaseDto;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractEditorContextMenu extends DefaultActionGroup {

    @Getter
    private static final @NotNull Set<UUID> globalPendingCutIds = new HashSet<>();

    @Getter
    @Setter
    private static boolean globalCutAction = false;

    @Getter
    @Setter
    private static @Nullable TestinEditor globalSourceEditorUI = null;

    public AbstractEditorContextMenu(final @NotNull String name, final boolean popup) {
        super(name, popup);
    }

    public static void clearCutState() {
        globalCutAction = false;
        globalPendingCutIds.clear();

        if (globalSourceEditorUI != null && globalSourceEditorUI.getPreferredFocusedComponent() != null) {
            globalSourceEditorUI.getPreferredFocusedComponent().repaint();
        }

        globalSourceEditorUI = null;
    }

    public abstract void registerShortcuts(final @NotNull JBList<TestCaseDto> list, final @NotNull AbstractEditorContextMenu menu);

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}