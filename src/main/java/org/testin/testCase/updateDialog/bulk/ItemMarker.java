package org.testin.testCase.updateDialog.bulk;

import com.intellij.openapi.editor.RangeMarker;
import org.jetbrains.annotations.Nullable;

/**
 * One editable array item in the bulk array editor: the live range marker plus
 * its owning test case / item indices. The snapshot offsets are render-time
 * values; navigation must use the live marker offsets.
 */
class ItemMarker {
    @Nullable RangeMarker marker;
    int tcIdx;
    int itemIdx;
    int startOffset;
    int endOffset;
}
