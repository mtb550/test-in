package org.testin.view.marker;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeCount;
import org.testin.model.NodeFigures;
import org.testin.ui.framework.DialogComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.List;

/**
 * A node's figures drawn as a ring, with the rate in the hole and the legend
 * beside it.
 * <p>
 * Drawn rather than depended on: there is no charting library in the project
 * and the platform charts API is not in this build, so this is a
 * {@code paintComponent} the way {@code BadgePill}, {@code TestCard} and
 * {@code Id} already paint themselves.
 * <p>
 * It draws whatever slices it is handed and asks nothing about where they came
 * from. A node whose numbers are not parts of one whole hands it none, and then
 * the ring is as tall as its legend, the legend has no rows, and the component
 * takes no room at all.
 * <p>
 * That is the same rule the details rows follow, where a blank value is simply
 * not a row. It is also why nothing here or in the dialog has to know that
 * containers have no chart.
 */
public final class VerdictDonut implements DialogComponent {

    /**
     * How thick the ring is drawn. Enough to read a color at a glance, and
     * little enough to leave the hole room for the number inside it.
     */
    private static final int THICKNESS = 14;

    /**
     * The smallest arc a verdict that happened is drawn as.
     * <p>
     * One failure in five hundred is three quarters of a degree, which at this
     * size is half a pixel of nothing. A verdict somebody recorded has to be
     * visible on the ring, so it takes this much of it - and every arc is then
     * scaled back down together so the ring still closes at 360.
     */
    private static final double MIN_SWEEP = 6.0;

    /**
     * Where the first arc starts: the top, going clockwise, the way a clock and
     * every other ring chart reads.
     */
    private static final int TOP = 90;

    private static final int ROW_HEIGHT = 22;

    /**
     * How wide the legend is: enough for the longest verdict name and a count
     * beside it, and no wider. It used to take whatever the dialog had, which
     * put the numbers against the far edge.
     */
    private static final int LEGEND_WIDTH = 200;

    /**
     * What separates the chart from the rows above it.
     */
    private static final int GAP = 12;

    private final @NotNull JBPanel<?> panel;

    public VerdictDonut(final @NotNull List<NodeCount> slices, final @NotNull NodeFigures figures) {
        final @NotNull JBPanel<?> legend = legend(slices, figures);
        final @NotNull Ring ring = new Ring(slices, figures);

        // The ring is as tall as its legend: the two say the same thing, so they
        // read as one block - and a legend with no rows leaves no ring to draw.
        final int side = legend.getPreferredSize().height;
        ring.setPreferredSize(new Dimension(side, side));
        ring.setMaximumSize(new Dimension(side, side));
        ring.setAlignmentY(Component.TOP_ALIGNMENT);
        legend.setAlignmentY(Component.TOP_ALIGNMENT);

        // Laid out left to right with the leftover width given to nothing at the
        // end, so the legend keeps its own width. Given the whole dialog it
        // stretched, and the counts ended up against the far right edge with the
        // names they belong to a hand's width away.
        final @NotNull JBPanel<?> block = new JBPanel<>();
        block.setLayout(new BoxLayout(block, BoxLayout.X_AXIS));
        block.setOpaque(false);
        block.add(ring);
        block.add(Box.createRigidArea(new Dimension(JBUI.scale(20), 0)));
        block.add(legend);
        block.add(Box.createHorizontalGlue());

        // Anchored to the top, the way the details rows above it are, so the
        // room the dialog has spare falls underneath the chart instead of
        // stretching it. Without this the rows took the space, the chart was
        // pushed to the bottom edge, and its last line was cut off.
        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        // The gap belongs to the chart, so a node that has no chart has no gap
        // either and the component is nothing at all in the dialog.
        panel.setBorder(JBUI.Borders.empty(side == 0 ? 0 : GAP, 0, 0, 16));
        panel.add(block, BorderLayout.NORTH);
    }

    /**
     * One row per slice: its color, its name, and its number.
     */
    private static @NotNull JBPanel<?> legend(final @NotNull List<NodeCount> slices, final @NotNull NodeFigures figures) {
        final @NotNull JBPanel<?> rows = new JBPanel<>();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        rows.setBorder(JBUI.Borders.empty());

        for (final NodeCount slice : slices) {
            rows.add(legendRow(slice, figures));
        }

        return rows;
    }

    private static @NotNull JBPanel<?> legendRow(final @NotNull NodeCount slice, final @NotNull NodeFigures figures) {
        final @NotNull JBPanel<?> row = new JBPanel<>(new BorderLayout(JBUI.scale(8), 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(JBUI.scale(LEGEND_WIDTH), JBUI.scale(ROW_HEIGHT)));
        row.setMaximumSize(new Dimension(JBUI.scale(LEGEND_WIDTH), JBUI.scale(ROW_HEIGHT)));

        final @NotNull JBLabel name = new JBLabel(slice.getCaption());
        name.setIcon(new Swatch(slice.getSwatch()));
        name.setIconTextGap(JBUI.scale(8));
        name.setFont(JBUI.Fonts.smallFont());

        final @NotNull JBLabel value = new JBLabel(slice.of(figures), SwingConstants.RIGHT);
        value.setFont(JBUI.Fonts.smallFont().asBold());

        row.add(name, BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);

        return row;
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return panel;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        // Display only - nothing to submit.
    }

    @Override
    public boolean wantsFocus() {
        return false;
    }

    /**
     * How many degrees each slice takes: its share of the whole, never less
     * than {@link #MIN_SWEEP} when it happened at all and always nothing when
     * it did not, scaled back together afterward so the ring still closes at
     * 360.
     * <p>
     * Here rather than inside the ring so it can be read as arithmetic and
     * tested as arithmetic: whether one failure in five hundred is visible is a
     * question about this method, not about a screenshot.
     */
    static double[] sweeps(final @NotNull List<NodeCount> slices, final @NotNull NodeFigures figures) {
        final double[] sweeps = new double[slices.size()];
        final double whole = Math.max(figures.run().total(), 1);
        double sum = 0;

        for (int i = 0; i < slices.size(); i++) {
            final long value = slices.get(i).valueIn(figures);
            sweeps[i] = value == 0 ? 0 : Math.max(value * 360.0 / whole, MIN_SWEEP);
            sum += sweeps[i];
        }

        final double scale = 360.0 / Math.max(sum, 360.0);
        for (int i = 0; i < sweeps.length; i++) {
            sweeps[i] *= scale;
        }

        return sweeps;
    }

    /**
     * The colored square that stands for a slice in the legend.
     */
    private record Swatch(@NotNull Color color) implements Icon {

        @Override
        public void paintIcon(final @NotNull Component owner, final @NotNull Graphics g, final int x, final int y) {
            g.setColor(color);
            g.fillRect(x, y, getIconWidth(), getIconHeight());
        }

        @Override
        public int getIconWidth() {
            return JBUI.scale(9);
        }

        @Override
        public int getIconHeight() {
            return JBUI.scale(9);
        }
    }

    /**
     * The ring itself: one arc per slice over a faint track, and the rate in
     * the middle.
     */
    private static final class Ring extends JComponent {

        private final @NotNull List<NodeCount> slices;
        private final @NotNull NodeFigures figures;

        private Ring(final @NotNull List<NodeCount> slices, final @NotNull NodeFigures figures) {
            this.slices = slices;
            this.figures = figures;
        }

        @Override
        protected void paintComponent(final @NotNull Graphics g) {
            final @NotNull Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(JBUI.scale(THICKNESS), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

                final int side = Math.min(getWidth(), getHeight());
                final double inset = JBUI.scale(THICKNESS) / 2.0;
                final double diameter = side - JBUI.scale(THICKNESS);

                paintTrack(g2, inset, diameter);
                paintArcs(g2, inset, diameter);
                paintRate(g2, side);
            } finally {
                g2.dispose();
            }
        }

        /**
         * The empty ring underneath, which is all a run with nothing recorded
         * leaves showing: the shape stays, so there is something to read the
         * hole against.
         */
        private void paintTrack(final @NotNull Graphics2D g2, final double inset, final double diameter) {
            g2.setColor(UIUtil.getPanelBackground().darker());
            g2.draw(new Arc2D.Double(inset, inset, diameter, diameter, 0, 360, Arc2D.OPEN));
        }

        private void paintArcs(final @NotNull Graphics2D g2, final double inset, final double diameter) {
            final double[] sweeps = sweeps(slices, figures);
            double start = TOP;

            for (int i = 0; i < slices.size(); i++) {
                g2.setColor(slices.get(i).getSwatch());
                g2.draw(new Arc2D.Double(inset, inset, diameter, diameter, start, -sweeps[i], Arc2D.OPEN));
                start -= sweeps[i];
            }
        }

        private void paintRate(final @NotNull Graphics2D g2, final int side) {
            final @NotNull String label = figures.rateLabel();

            g2.setFont(JBUI.Fonts.label().deriveFont(Font.BOLD, JBUIScale.scaleFontSize(15f)));
            g2.setColor(UIUtil.getLabelForeground());

            final @NotNull FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(label,
                    (side - metrics.stringWidth(label)) / 2f,
                    (side - metrics.getHeight()) / 2f + metrics.getAscent());
        }
    }
}
