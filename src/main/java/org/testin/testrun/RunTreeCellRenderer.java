package org.testin.testrun;

import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SimpleTextAttributes;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.TestStatus;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;

import javax.swing.*;
import java.util.Map;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RunTreeCellRenderer {

    public static @NotNull CheckboxTree.CheckboxTreeCellRenderer create(final @NotNull Map<@NotNull UUID, @NotNull TestRunItems> resultsMap) {
        return new CheckboxTree.CheckboxTreeCellRenderer() {
            // Both @NotNull because the platform says so, not because it looks
            // right: CheckboxTreeCellRendererBase is Kotlin, and its bytecode
            // calls Intrinsics.checkNotNullParameter on tree and on value. It
            // already throws on null, so the annotations cannot add a crash -
            // which is the check that was missing when an unverified @NotNull on
            // a renderer parameter caused the paint crash fixed in 92f1a1ed.
            @Override
            public void customizeRenderer(final @NotNull JTree tree, final @NotNull Object value, final boolean selected, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
                if (value instanceof CheckedTreeNode node) {
                    final @Nullable Object userObj = node.getUserObject();

                    if (userObj instanceof DirectoryDto dir)
                        getTextRenderer().append(dir.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

                    else if (userObj instanceof TestCaseDto tc) {
                        final @Nullable TestRunItems result = resultsMap.get(tc.getId());

                        if (result != null) {
                            final @NotNull TestStatus status = result.getStatus();
                            getTextRenderer().append(tc.getDescription(), status.getStyle());
                            getTextRenderer().append(status.getDisplayText(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                        } else
                            getTextRenderer().append(tc.getDescription(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

                    } else if (userObj instanceof String str)
                        getTextRenderer().append(str, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                }
            }
        };
    }
}
