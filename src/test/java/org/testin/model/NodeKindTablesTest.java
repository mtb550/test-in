package org.testin.model;

import org.jetbrains.annotations.NotNull;
import org.testin.codegen.JavaCode;
import org.testin.creator.NodeCreators;
import org.testin.remove.Removals;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * The tables that used to be columns still answer for every kind of node.
 * <p>
 * {@link DirectoryType} named a creator, three code generators and a remove
 * handler for each of its kinds, and that made the vocabulary package import
 * four features (#111). Each of those is now an enum of its own in the package
 * that knows the answer, with a constant per kind, named after it - and the
 * bridge is {@code valueOf(type.name())}, which is why the names matter.
 * <p>
 * What was lost in the move is the constructor: the old enum could not be
 * extended without every column being supplied, and nothing forces that across
 * two files. This test is what forces it. It fails on the day an eighth kind of
 * node is declared and one of the tables is not told, which is the only way that
 * mistake can be made.
 */
public class NodeKindTablesTest {

    private static final @NotNull List<String> KINDS =
            Arrays.stream(DirectoryType.values()).map(Enum::name).collect(Collectors.toList());

    @Test
    public void everyKindOfNodeSaysWhatMakesIt() {
        assertSameNames(NodeCreators.class, "org.testin.creator.NodeCreators");
    }

    @Test
    public void everyKindOfNodeSaysWhatItsJavaDoes() {
        assertSameNames(JavaCode.class, "org.testin.codegen.JavaCode");
    }

    @Test
    public void everyKindOfNodeSaysHowItIsRemoved() {
        assertSameNames(Removals.class, "org.testin.remove.Removals");
    }

    /**
     * The same guarantee one level down: {@code NodeCounter} names one way of
     * gathering figures per {@link NodeStatistics}, and reaches it by name too.
     * The enum is private to the counter, which is right - nothing else has any
     * business with it - so this is the one place that looks.
     */
    @Test
    public void everyWayOfCountingIsGathered() {
        final @NotNull Set<String> gathered = constantsOf("org.testin.indexer.NodeCounter$Gathered");

        assertEquals(gathered, Arrays.stream(NodeStatistics.values()).map(Enum::name).collect(Collectors.toSet()),
                "NodeCounter.Gathered and NodeStatistics no longer name the same ways of counting,"
                        + " so NodeCounter.figures throws on a node counted the way that is missing");
    }

    private static void assertSameNames(final @NotNull Class<?> table, final @NotNull String named) {
        final @NotNull List<String> names =
                Arrays.stream(table.getEnumConstants()).map(c -> ((Enum<?>) c).name()).collect(Collectors.toList());

        assertEquals(names, KINDS,
                named + " no longer has one constant per DirectoryType, named after it and in the same order."
                        + " A kind of node with no entry throws on valueOf the first time a tester reaches it (#111)");
    }

    private static @NotNull Set<String> constantsOf(final @NotNull String className) {
        try {
            return Arrays.stream(Class.forName(className).getEnumConstants())
                    .map(c -> ((Enum<?>) c).name())
                    .collect(Collectors.toSet());

        } catch (final ClassNotFoundException ex) {
            fail(className + " is gone, so nothing checks that the ways of counting still match");
            return Set.of();
        }
    }
}
