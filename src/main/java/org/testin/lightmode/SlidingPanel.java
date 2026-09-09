package org.testin.lightmode;

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-201.
 * <p>
 * The case block, which slides down out of the way while the next case comes
 * in from above.
 * <p>
 * A tester who has just pressed a verdict key is looking at the application
 * under test, not at this window, and the old window replaced one case with the
 * next in a single repaint - so the only evidence a verdict had been taken was
 * that the words were now different words. The movement is what says a case
 * went and another arrived (#178).
 * <p>
 * <b>The one leaving is a picture, the one arriving is the panel itself.</b>
 * Painting the outgoing case from a snapshot is what makes this cheap: the
 * fields have already been overwritten with the new case by the time the first
 * frame is drawn, so there is nothing left to ask for the old layout. The
 * incoming case is drawn by the ordinary paint, offset - so it wraps, scales
 * with the zoom and picks up the theme exactly as it does when it is standing
 * still.
 * <p>
 * The two are offset by the same distance in opposite directions, so they tile
 * against each other with no gap and no overlap at any point of the slide.
 */
final class SlidingPanel extends JBPanel<SlidingPanel> {

    /**
     * How the case that is leaving looked, and empty whenever nothing is
     * sliding - which is almost always, and is the state that costs nothing.
     */
    private @NotNull Optional<Image> leaving = Optional.empty();

    /** 0 at the start of a slide, 1 when the new case is in place. */
    private double travelled = 1.0;

    SlidingPanel(final @NotNull LayoutManager layout) {
        super(layout);
    }

    /**
     * Remembers how the case on screen looks, before the caller writes the next
     * one over it.
     * <p>
     * Nothing is remembered for a panel with no size - the window before it is
     * first shown - and nothing then slides, which is right: there is no case
     * leaving when the window has only just opened.
     */
    void captureLeaving() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            leaving = Optional.empty();
            return;
        }

        final @NotNull BufferedImage image = UIUtil.createImage(this, getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        final @NotNull Graphics2D g = image.createGraphics();

        // super, not this: painting through the override would ask the snapshot
        // to include the slide it is being taken for.
        super.paint(g);
        g.dispose();

        leaving = Optional.of(image);
    }

    /**
     * How far through the slide this is, from the animation. Anything at 1 or
     * beyond ends it and lets the picture go.
     */
    void setTravelled(final double fraction) {
        travelled = fraction;

        if (fraction >= 1.0) leaving = Optional.empty();

        repaint();
    }

    /** Whether a slide is worth starting, which it is not before the first case. */
    boolean hasSomethingToSlide() {
        return getWidth() > 0 && getHeight() > 0;
    }

    @Override
    public void paint(final @NotNull Graphics g) {
        if (travelled >= 1.0 || leaving.isEmpty()) {
            super.paint(g);
            return;
        }

        final int height = getHeight();

        // The case leaving, on its way down and out. Drawn first and drawn
        // whole: the panel is not opaque, so the case arriving does not paint a
        // background over it.
        leaving.ifPresent(image -> UIUtil.drawImage(g, image, 0, (int) (height * travelled), null));

        // The case arriving, still above the panel and coming down into place.
        // Clipped to the panel's own bounds by the graphics it was handed.
        final @NotNull Graphics2D arriving = (Graphics2D) g.create();
        arriving.translate(0, (int) (-height * (1.0 - travelled)));
        super.paint(arriving);
        arriving.dispose();
    }
}
