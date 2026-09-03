package org.testin.editor;

import org.testin.model.DirectoryType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.editor.test.TestEditor;

import javax.swing.*;
import java.util.function.BiFunction;

public class EditorType extends FakeFileType {

    public static final @NotNull EditorType TEST_RUN = new EditorType(
            DirectoryType.TR.getDisplayedName(),
            "Test Run Editor",
            AllIcons.Nodes.Services,
            RunEditor::new
    );

    public static final @NotNull EditorType TEST_CASE = new EditorType(
            "Test Case",
            "Test Case Editor",
            AllIcons.FileTypes.Text,
            TestEditor::new
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

    @Getter
    @NotNull
    private final BiFunction<Project, UnifiedVirtualFile, TestinEditor> factory;

    private EditorType(final @NotNull String name, final @NotNull String description, final @NotNull Icon icon, final @NotNull BiFunction<Project, UnifiedVirtualFile, TestinEditor> factory) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.factory = factory;
    }

    /**
     * Which editor a node opens in.
     * <p>
     * Here rather than on {@link DirectoryType}, which is where #175 proposed it:
     * an EditorType carries the constructor of the editor it opens, so declaring
     * one on the enum would make {@code model} import {@code editor} - the leaf
     * rule that same issue lists as a criterion, and #111's whole subject. The
     * mapping runs this way instead, since {@code editor} may know about
     * {@code model} and not the reverse.
     * <p>
     * Only the kinds that open in an editor reach here; the caller has already
     * asked {@code isOpenableInEditor}.
     */
    public static @NotNull EditorType of(final @NotNull DirectoryDto dir) {
        return dir.getType() == DirectoryType.TR ? TEST_RUN : TEST_CASE;
    }

    @Override
    public boolean isMyFileType(final @NotNull VirtualFile file) {
        return false;
    }

}
