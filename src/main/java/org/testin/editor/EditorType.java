package org.testin.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.editor.test.TestEditor;
import org.testin.model.DirectoryType;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.util.Bundle;

import javax.swing.*;
import java.util.function.BiFunction;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class EditorType extends FakeFileType {

    /**
     * UC-EDITOR-PANEL-001.
     * <p>
     * The name is a literal and stays one in every language. This is a
     * {@code FileType}, and the platform treats {@code getName()} as its
     * identity: it must be unique among every file type registered in the IDE
     * and it is what the IDE writes down when it remembers a file-type
     * association. It took {@code DirectoryType.TR.getDescription()} until that
     * became a translated string, which would have given the type a different
     * identity in every language (#11).
     * <p>
     * The description beside it is the localizable half, and the platform says
     * so by annotating it {@code @Nls}.
     */
    public static final @NotNull EditorType TEST_RUN = new EditorType(
            "Test Run",
            Bundle.message("editor.type.run.description"),
            AllIcons.Nodes.Services,
            RunEditor::new
    );

    public static final @NotNull EditorType TEST_CASE = new EditorType(
            "Test Case",
            Bundle.message("editor.type.case.description"),
            AllIcons.FileTypes.Text,
            TestEditor::new
    );

    private final @NotNull String name;
    private final @NotNull String description;
    private final @NotNull Icon icon;
    private final @NotNull BiFunction<Project, UnifiedVirtualFile, TestinEditor> factory;

    /**
     * UC-EDITOR-PANEL-001, Rule-EDITOR-PANEL-001.
     * <p>
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
