package org.testin.editorPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class EditorType extends FakeFileType {

    public static final EditorType TEST_RUN = new EditorType(
            "Test Run",
            "Test Run Editor",
            AllIcons.Nodes.Services
    );

    public static final EditorType TEST_CASE = new EditorType(
            "Test Case",
            "Test Case Editor",
            AllIcons.FileTypes.Text
    );

    @Getter
    @NotNull
    private final String name;

    @Getter
    @NotNull
    private final String description;

    @Getter
    @NotNull
    private final Icon icon;

    private EditorType(final @NotNull String name, final @NotNull String description, final @NotNull Icon icon) {
        this.name = name;
        this.description = description;
        this.icon = icon;
    }

    @Override
    public boolean isMyFileType(final @NotNull VirtualFile file) {
        return false;
    }

}