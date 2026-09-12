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

import com.intellij.icons.AllIcons;
import com.intellij.ui.SimpleTextAttributes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
import org.testin.util.NameSanitizer;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum DirectoryType {
    TP(
            Bundle.message("node.tp"),
            "",
            AllIcons.Nodes.Project,
            ".tp",
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            List.of(NodeCount.TEST_SETS, NodeCount.PACKAGES, NodeCount.TEST_CASES, NodeCount.TEST_RUNS)
    ),

    TCD(
            Bundle.message("node.tcd"),
            "Test Cases",
            AllIcons.Nodes.Bookmark,
            ".tcd",
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            List.of(NodeCount.TEST_SETS, NodeCount.PACKAGES, NodeCount.TEST_CASES)
    ),

    TRD(
            Bundle.message("node.trd"),
            "Test Runs",
            AllIcons.Nodes.Bookmark,
            ".trd",
            SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            List.of(NodeCount.PACKAGES, NodeCount.TEST_RUNS)
    ),

    TSP(
            Bundle.message("node.tsp"),
            "",
            AllIcons.Nodes.WebFolder,
            ".tsp",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            List.of(NodeCount.TEST_SETS, NodeCount.PACKAGES, NodeCount.TEST_CASES)
    ),

    TRP(
            Bundle.message("node.trp"),
            "",
            AllIcons.Nodes.WebFolder,
            ".trp",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            List.of(NodeCount.PACKAGES, NodeCount.TEST_RUNS)
    ),

    TS(
            Bundle.message("node.ts"),
            "",
            AllIcons.Vcs.Changelist,
            ".ts",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            NodeStatistics.CHILDREN,
            List.of(NodeCount.TEST_CASES)
    ),

    TR(
            Bundle.message("node.tr"),
            "",
            AllIcons.Toolwindows.ToolWindowRunWithCoverage,
            ".tr",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            NodeStatistics.VERDICTS,
            List.of(NodeCount.TOTAL)
    );


    /**
     * UC-TREE-PANEL-014, Rule-TREE-PANEL-049.
     * <p>
     * What may be dropped or pasted into each kind of node, as one table.
     * <p>
     * It was five predicates over seven files until #176: {@code
     * isTransferTarget} on the target, {@code acceptsTransferred} overridden
     * five times, and three {@code isAllowedIn...} questions that existed only
     * to be asked by those overrides and by nothing else. One fact - which kinds
     * go inside which - answered in thirteen method bodies, where no reader
     * could see the whole of it and no two rows could be compared.
     * <p>
     * A test run accepts nothing, which is the one thing this table changed. It
     * answered {@code isTransferTarget() == true} before and refused every
     * source that reached it, so the tree flashed a drop highlight over a node
     * that was never going to take the drop.
     * <p>
     * Empty for the three that take nothing: a test project holds its two fixed
     * containers and nothing a tester puts there, a test set holds test cases
     * rather than nodes, and a run holds run items.
     */
    private static final @NotNull Map<DirectoryType, Set<DirectoryType>> ACCEPTS = Map.of(
            TP, Set.of(),
            TCD, Set.of(TS, TSP),
            TRD, Set.of(TR, TRP),
            TSP, Set.of(TS, TSP),
            TRP, Set.of(TR, TRP),
            TS, Set.of(),
            TR, Set.of());

    /**
     * Whether a node of that kind may be dropped or pasted into a node of this
     * one.
     */
    public boolean accepts(final @NotNull DirectoryType source) {
        return ACCEPTS.getOrDefault(this, Set.of()).contains(source);
    }

    /**
     * Whether anything at all may be put into this kind - what the tree asks
     * before it draws a drop highlight.
     * <p>
     * {@code getOrDefault} rather than {@code get}: a kind missing from the
     * table takes nothing, which is the safe answer, and {@code
     * NodeKindTablesTest} is what says none is missing.
     */
    public boolean acceptsAnything() {
        return !ACCEPTS.getOrDefault(this, Set.of()).isEmpty();
    }

    /**
     * What a directory directly under Test Cases may be marked as, and in which
     * order to ask.
     * <p>
     * The order is the precedence, and it was written out at four call sites and
     * written down at none: a directory carrying both markers is read as a test
     * set, because a set is what holds cases and a package is what holds sets.
     * Anyone probing in the other order would have got a different tree and no
     * warning about it (#173).
     */
    public static final @NotNull List<DirectoryType> UNDER_TEST_CASES = List.of(TS, TSP);

    /**
     * The same question on the run side, with the same precedence rule.
     */
    public static final @NotNull List<DirectoryType> UNDER_TEST_RUNS = List.of(TR, TRP);

    /**
     * UC-TREE-PANEL-008, Rule-TREE-PANEL-095.
     * <p>
     * The kinds whose name becomes a Java package - Rule-CODEGEN-008, every
     * folder above a test set.
     * <p>
     * The test set itself is not one of them: its name becomes the class, which
     * always ends in {@code Test} and so is never a word Java keeps for itself.
     * The run family generates no code at all, so a test run may be called
     * anything a folder may be called.
     * <p>
     * A list beside the two above it rather than a thirteenth column on the
     * enum. It is one fact about two constants, and saying "no" seven times in a
     * constructor argument is harder to read than the two names are.
     */
    public static final @NotNull List<DirectoryType> BECOME_JAVA_PACKAGES = List.of(TP, TSP);

    /**
     * UC-TREE-PANEL-008, Rule-TREE-PANEL-095.
     * <p>
     * Whether a node of this kind may be called that - asked by the create and
     * rename dialogs before the name is stored.
     * <p>
     * Asked of the type rather than tested for at the dialogs, so the three that
     * ask cannot come to three different answers, and a kind of node added later
     * is covered by the list above rather than by remembering to edit them
     * (#11).
     */
    public boolean canTakeName(final @NotNull String name) {
        return !BECOME_JAVA_PACKAGES.contains(this) || NameSanitizer.canMakePackageName(name);
    }

    /**
     * The markers a family is recognized by, joined the way a warning says them -
     * {@code .ts/.tsp}. Asked rather than typed, so a marker renamed here does not
     * leave a log line describing the name it used to have.
     */
    public static @NotNull String markerNames(final @NotNull List<DirectoryType> family) {
        return family.stream().map(DirectoryType::getMarker).collect(Collectors.joining("/"));
    }

    /**
     * What this kind is called in a log line - the description in lower case, so
     * the word and its capitalized form cannot drift apart. Every reader of a
     * marker used to be handed this word by hand, beside the marker's file name
     * and the class it parses to: three facts about one thing, spelled out at
     * seven call sites, and nothing checking they belonged together (#173).
     */
    public @NotNull String getMarkerKind() {
        return description.toLowerCase(Locale.ROOT);
    }

    private final @NotNull String description;

    /**
     * UC-CODEGEN-001, Rule-CODEGEN-008.
     * <p>
     * <b>The folder this kind of node lives in on disk, and never anything a
     * tester reads.</b> Empty for every kind but the two fixed containers, which
     * are the only ones whose folder Testin names rather than the tester.
     * <p>
     * It was called {@code displayedName} and described as a label, and it is
     * not one: all nine callers resolve a path with it, set a DTO name from it,
     * build a {@code path2} out of it or strip it back off again in
     * {@link org.testin.codegen.Fqcn}. Not one of them draws anything. The tree
     * shows it only because the DTO takes its name from here.
     * <p>
     * The name mattered the moment the plugin was to be translated. "Test Cases"
     * read as a caption, and a caption is exactly what gets translated - which
     * would have pointed every existing test project at a folder called
     * something else, and changed the package every generated class is written
     * into. Called what it is, it is obviously not a string to translate (#11).
     */
    private final @NotNull String folderName;
    private final @NotNull Icon icon;
    private final @NotNull String marker;

    private final @NotNull SimpleTextAttributes attributes;

    /**
     * Which of the two ways of counting a node this kind is counted by, and so
     * what its Details draws - see {@link NodeStatistics}. Declared here beside
     * the icon because it is the same kind of fact: something true of the type,
     * answered once, rather than a question the Details dialog asks about the
     * node in front of it (#82).
     */
    private final @NotNull NodeStatistics statistics;

    /**
     * The counts this kind of node reports as rows in its Details, in order.
     * <p>
     * A type lists only what can apply to it. Nothing on the test-case side can
     * hold a run and nothing on the run side can hold a test set, so those are
     * impossible states rather than choices, and a {@code 0} there would imply
     * it could be otherwise. A test run lists its total alone: the rest of its
     * numbers are the chart, and printing them twice would be the popup
     * disagreeing with itself about which one to read.
     */
    private final @NotNull List<NodeCount> counts;
}
