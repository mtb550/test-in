package org.testin.testcase.updateDialog.bulk;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What both bulk JSON editors need: the escaping their editor text is written
 * in, the appearance of the editors themselves, and caret placement across the
 * editable ranges.
 * <p>
 * Each dialog carried its own copy, so a fix to the escaping in one left the
 * other writing different text back into storage.
 */
final class BulkJsonEditor {

    private BulkJsonEditor() {
    }

    /**
     * Newlines flatten to spaces: the editor shows one value per line, so a
     * multi-line value would otherwise break the JSON shape being edited.
     */
    static @NotNull String escapeJson(final @Nullable String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    static @NotNull String unescapeJson(final @Nullable String str) {
        if (str == null) return "";
        return str.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    /**
     * JSON highlighting, a larger font, line numbers and no soft wraps - applied
     * to the read-only original on the left and the editable copy on the right.
     */
    static void setupEditorAppearance(final @NotNull Editor editor, final @NotNull Project p) {
        final FileType jsonFileType = FileTypeManager.getInstance().getFileTypeByExtension("json");
        final EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(p, jsonFileType);

        if (editor instanceof EditorEx)
            ((EditorEx) editor).setHighlighter(highlighter);

        final EditorColorsScheme scheme = editor.getColorsScheme();
        scheme.setEditorFontSize(15f);
        scheme.setLineSpacing(1.4f);

        final EditorSettings settings = editor.getSettings();
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
        final CaretModel caretModel = editor.getCaretModel();
        caretModel.removeSecondaryCarets();

        boolean isFirst = true;
        for (final RangeMarker marker : markers) {
            if (!marker.isValid()) continue;

            final LogicalPosition logPos = editor.offsetToLogicalPosition(marker.getEndOffset());
            final VisualPosition visPos = editor.logicalToVisualPosition(logPos);

            if (isFirst) {
                caretModel.moveToVisualPosition(visPos);
                isFirst = false;
            } else {
                caretModel.addCaret(visPos, true);
            }
        }
    }
}
