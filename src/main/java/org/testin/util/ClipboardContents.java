package org.testin.util;

import com.intellij.openapi.ide.CopyPasteManager;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.Optional;

/**
 * What the system clipboard is holding.
 * <p>
 * An empty clipboard is a null from the platform, and four places tested for it
 * separately: the tree's transfer handler, the two paste actions and the text
 * area that turns a pasted image into a data URI. Each also asked whether the
 * contents carried the flavor it wanted, so each carried the same two-part
 * condition.
 * <p>
 * Both questions are answered here, so an empty clipboard and a clipboard
 * holding somebody else's data mean the same thing everywhere: nothing to
 * paste.
 */
public final class ClipboardContents {

    private ClipboardContents() {
    }

    public static @NotNull Optional<Transferable> current() {
        return Optional.ofNullable(CopyPasteManager.getInstance().getContents());
    }

    /**
     * The clipboard's contents when they carry the given flavor, and nothing
     * when the clipboard is empty or holds something else.
     */
    public static @NotNull Optional<Transferable> withFlavor(final @NotNull DataFlavor flavor) {
        return current().filter(contents -> contents.isDataFlavorSupported(flavor));
    }
}
