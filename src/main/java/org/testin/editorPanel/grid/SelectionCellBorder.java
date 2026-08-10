package org.testin.editorPanel.grid;

import org.testin.editorPanel.EditorColors;

import javax.swing.border.Border;
import java.awt.*;

/**
 * Selection border for grid cells; keeps the same insets as an unselected cell
 * so selection never changes the cell width or wrapping.
 */
record SelectionCellBorder(Insets insets) implements Border {

    SelectionCellBorder(final boolean firstColumn) {
        this(new Insets(1, firstColumn ? 1 : 0, 1, 1));
    }

    @Override
    public Insets getBorderInsets(final Component component) {
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    @Override
    public void paintBorder(final Component component, final Graphics graphics, final int x, final int y, final int width, final int height) {
        final Color previousColor = graphics.getColor();
        graphics.setColor(EditorColors.SELECTION_BORDER);
        graphics.drawRect(x, y, width - 1, height - 1);
        graphics.setColor(previousColor);
    }
}
