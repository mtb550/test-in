package org.testin.testcase.update.bulk;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * What both bulk JSON editors need: the escaping their editor text is written
 * in, the appearance of the editors themselves, and caret placement across the
 * editable ranges.
 * <p>
 * Each dialog carried its own copy, so a fix to the escaping in one left the
 * other writing different text back into storage.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BulkJsonEditor {

    /**
     * A value as it is written inside the JSON these dialogs show.
     * <p>
     * A line break becomes the two characters {@code \n}. It used to become a
     * space, which was deliberate and lossy: the value sits on one line of the
     * editor either way, so flattening it looked free. It was free only until
     * the tester edited that row - then the flattened text was what got saved,
     * and a multi-line expected result came back as one line with nothing said.
     * Test Data became a multi-line field as well, which turned a rare loss
     * into a likely one.
     * <p>
     * A carriage return is still dropped rather than escaped. That is a
     * normalization to the one line ending the stored JSON uses, not content
     * going missing.
     */
    static @NotNull String escapeJson(final @NotNull String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    /**
     * The inverse, read left to right in one pass.
     * <p>
     * Not a sequence of replaces, which is what it was and what a third escape
     * broke. A value holding a backslash followed by an n is written as
     * {@code \\n}, and a replace looking for {@code \n} finds it inside that
     * pair and hands back a line break the tester never typed. Reading forward,
     * the first backslash consumes the second and the n is only an n.
     */
    static @NotNull String unescapeJson(final @NotNull String str) {
        final @NotNull StringBuilder out = new StringBuilder(str.length());

        for (int i = 0; i < str.length(); i++) {
            final char current = str.charAt(i);

            if (current != '\\' || i == str.length() - 1) {
                out.append(current);
                continue;
            }

            final char escaped = str.charAt(++i);

            switch (escaped) {
                case 'n' -> out.append('\n');
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                // Not an escape this writes, so it is the two characters the
                // tester typed and it goes back as both.
                default -> out.append(current).append(escaped);
            }
        }

        return out.toString();
    }

    /**
     * JSON highlighting, a larger font, line numbers and no soft wraps - applied
     * to the read-only original on the left and the editable copy on the right.
     */
    static void setupEditorAppearance(final @NotNull Editor editor, final @NotNull Project p) {
        final @NotNull FileType jsonFileType = FileTypeManager.getInstance().getFileTypeByExtension("json");
        final @NotNull EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(p, jsonFileType);

        if (editor instanceof EditorEx)
            ((EditorEx) editor).setHighlighter(highlighter);

        final @NotNull EditorColorsScheme scheme = editor.getColorsScheme();
        scheme.setEditorFontSize(15f);
        scheme.setLineSpacing(1.4f);

        final @NotNull EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setLineMarkerAreaShown(false);
        settings.setFoldingOutlineShown(true);
        settings.setVirtualSpace(false);
        settings.setUseSoftWraps(false);
        settings.setAdditionalLinesCount(1);
    }

    /**
     * The editable offset nearest to where the caret landed, so clicking the
     * guarded JSON around a value does not leave the caret somewhere it cannot type.
     */
    static int nearestValidOffset(final int offset, final @NotNull List<RangeMarker> markers) {
        int minDistance = Integer.MAX_VALUE;
        int nearestOffset = offset;
        for (final RangeMarker m : markers) {
            // An invalidated marker still answers getStartOffset, with an offset
            // the document no longer has. placeCaretOnAll below already skips
            // them; one caller filtered before calling and the other did not, so
            // the check belongs here rather than in the callers' hands.
            if (!m.isValid()) continue;

            if (Math.abs(offset - m.getStartOffset()) < minDistance) {
                minDistance = Math.abs(offset - m.getStartOffset());
                nearestOffset = m.getStartOffset();
            }
            if (Math.abs(offset - m.getEndOffset()) < minDistance) {
                minDistance = Math.abs(offset - m.getEndOffset());
                nearestOffset = m.getEndOffset();
            }
        }
        return nearestOffset;
    }

    /**
     * Ctrl+Shift+A: one caret at the end of every editable value.
     */
    static void placeCaretOnAll(final @NotNull Editor editor, final @NotNull List<RangeMarker> markers) {
        final @NotNull CaretModel caretModel = editor.getCaretModel();
        caretModel.removeSecondaryCarets();

        boolean isFirst = true;
        for (final RangeMarker marker : markers) {
            if (!marker.isValid()) continue;

            final @NotNull LogicalPosition logPos = editor.offsetToLogicalPosition(marker.getEndOffset());
            final @NotNull VisualPosition visPos = editor.logicalToVisualPosition(logPos);

            if (isFirst) {
                caretModel.moveToVisualPosition(visPos);
                isFirst = false;
            } else {
                caretModel.addCaret(visPos, true);
            }
        }
    }
}
