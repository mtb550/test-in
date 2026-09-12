/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.model;

import org.jetbrains.annotations.NotNull;
import org.testin.codegen.JavaCode;
import org.testin.creator.NodeCreators;
import org.testin.remove.Removals;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
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

    /**
     * UC-TREE-PANEL-014, Rule-TREE-PANEL-049.
     * <p>
     * The drop table names every kind, so a kind added without a row does not
     * quietly accept nothing - which is a node that refuses every drag with no
     * way to tell that from a decision (#176).
     */
    @Test
    public void everyKindOfNodeSaysWhatItAccepts() {
        assertEquals(constantsOf(DirectoryType.class, "ACCEPTS"),
                Arrays.stream(DirectoryType.values()).map(Enum::name).collect(Collectors.toSet()),
                "DirectoryType.ACCEPTS does not have a row per kind, and a kind with no row accepts nothing");
    }

    /**
     * What goes inside what, asserted rather than described.
     * <p>
     * The rule is two families that never mix - test sets and their packages on
     * one side, runs and their packages on the other - and three kinds that take
     * nothing at all: a test project holds its two fixed containers, a test set
     * holds test cases, and a run holds run items.
     */
    @Test
    public void theTwoFamiliesNeverMix() {
        assertTrue(DirectoryType.TCD.accepts(DirectoryType.TS), "a test set belongs under Test Cases");
        assertTrue(DirectoryType.TSP.accepts(DirectoryType.TSP), "a package belongs in a package");
        assertTrue(DirectoryType.TRD.accepts(DirectoryType.TR), "a run belongs under Test Runs");
        assertTrue(DirectoryType.TRP.accepts(DirectoryType.TR), "a run belongs in a run package");

        assertFalse(DirectoryType.TCD.accepts(DirectoryType.TR), "a run does not belong under Test Cases");
        assertFalse(DirectoryType.TRD.accepts(DirectoryType.TS), "a test set does not belong under Test Runs");
        assertFalse(DirectoryType.TSP.accepts(DirectoryType.TRP), "a run package does not belong in a set package");

        for (final DirectoryType source : DirectoryType.values()) {
            assertFalse(DirectoryType.TR.accepts(source), "a test run holds run items, so " + source + " cannot be dropped into it");
            assertFalse(DirectoryType.TS.accepts(source), "a test set holds test cases, so " + source + " cannot be dropped into it");
            assertFalse(DirectoryType.TP.accepts(source), "a test project holds its two containers, so " + source + " cannot be dropped into it");
        }

        assertFalse(DirectoryType.TR.acceptsAnything(), "a test run takes nothing, so the tree must not draw a drop highlight over one");
    }

    private static void assertSameNames(final @NotNull Class<?> table, final @NotNull String named) {
        final @NotNull List<String> names =
                Arrays.stream(table.getEnumConstants()).map(c -> ((Enum<?>) c).name()).collect(Collectors.toList());

        assertEquals(names, KINDS,
                named + " no longer has one constant per DirectoryType, named after it and in the same order."
                        + " A kind of node with no entry throws on valueOf the first time a tester reaches it (#111)");
    }

    /**
     * The keys of a private static map on a class, by name. Reflection because
     * the table is the class's own business - nothing but this test has any use
     * for its shape.
     */
    private static @NotNull Set<String> constantsOf(final @NotNull Class<?> owner, final @NotNull String field) {
        try {
            final @NotNull java.lang.reflect.Field declared = owner.getDeclaredField(field);
            declared.setAccessible(true);

            return ((Map<?, ?>) declared.get(null)).keySet().stream()
                    .map(key -> ((Enum<?>) key).name())
                    .collect(Collectors.toSet());

        } catch (final ReflectiveOperationException ex) {
            fail(owner.getSimpleName() + "." + field + " is gone, so nothing checks that every kind has a row");
            return Set.of();
        }
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
