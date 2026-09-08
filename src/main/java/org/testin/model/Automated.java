package org.testin.model;

import com.intellij.icons.AllIcons;
import com.intellij.ui.LayeredIcon;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * Whether a test case has automation behind it, and what that looks like.
 * <p>
 * Derived, never stored: the answer is a generated method carrying the case's
 * id, so it changes when the code changes and nothing about it belongs in a
 * test case's JSON.
 * <p>
 * Three icons rather than three colors. The request asked for blue, red and
 * white, and white is invisible on a light theme - a plain color on a platform
 * icon is not theme-aware either, and hand-tinting one is painting nothing
 * tests. The platform already draws a class, a class with something wrong, and
 * a class that is declared and not implemented, in whatever theme the tester is
 * using.
 * <p>
 * Shapes, not shades. Fading the one icon was tried first and drew a gray
 * circle nobody could read - a state has to be told apart by its outline, not
 * by how bright it is.
 * <p>
 * All three are the same size on purpose. {@code Shared.descriptionActionIcons}
 * and {@code Shared.titleColumnWidth} both measure the card's title column from
 * this icon's width, so a state whose icon were wider would move the title
 * column on the cards in that state and nowhere else.
 */
@Getter
@AllArgsConstructor
public enum Automated {

    /**
     * Not read yet, or an IDE with no Java plugin. Drawn exactly as the button
     * has always been drawn, so a card says nothing it does not know - a case
     * reported as un-automated because nobody has looked yet would be worse
     * than saying nothing at all.
     */
    UNKNOWN("Navigate to Code", AllIcons.Nodes.Class),

    /**
     * A generated method carries this case's id.
     */
    WRITTEN("Automated", AllIcons.Nodes.Class),

    /**
     * The case names a method and none carries its id: the automation was
     * written and is gone, or was never generated. Something to fix.
     */
    MISSING("No test method", LayeredIcon.create(AllIcons.Nodes.Class, AllIcons.Nodes.ErrorMark)),

    /**
     * Nothing has been automated here. Either the case names no method at all,
     * because it has no description (Rule-CODEGEN-002), or Testin wrote one and
     * nobody has filled it in.
     * <p>
     * An abstract class icon, which the platform draws hollow: declared and not
     * implemented is exactly this state, and it is a shape rather than a shade.
     * The first try faded the class icon with {@code getDisabledIcon} and that
     * rendered as a gray circle - unreadable, and telling a tester nothing.
     */
    NONE("Not automated", AllIcons.Nodes.AbstractClass);

    /**
     * The states a tester can narrow a list to, in the order the filter offers
     * them. {@link #UNKNOWN} is not one: nobody can look for the test cases
     * nobody has looked at.
     */
    public static final @NotNull List<Automated> FILTERABLE = List.of(WRITTEN, MISSING, NONE);

    /**
     * What the tooltip says while the pointer is over the icon.
     */
    private final @NotNull String label;

    private final @NotNull Icon icon;

}
