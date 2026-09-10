package org.testin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;

import java.util.List;
import java.util.Optional;

/**
 * What a Testin surface has selected, asked the way the platform asks (#119).
 * <p>
 * <b>Why this exists at all.</b> An action declared in {@code plugin.xml} is
 * built by the platform with a no-arg constructor, so it cannot be handed the
 * tree or the list the way every action here is handed one today. It has to ask
 * the surface that has the keyboard, and the platform's word for that is a
 * {@link DataKey}. Nothing in this plugin published one before, which is why
 * declaring an action was never as small as adding a line of XML.
 * <p>
 * <b>One owner for the keys and for the questions.</b> A key is a string at
 * bottom, and two files spelling the same string is a lookup that silently
 * answers nothing. The three constants are here and nowhere else, and so are the
 * questions an action asks of them - so an action never writes
 * {@code e.getData(...)} and never has to decide what an absent answer means.
 * <p>
 * Every question answers {@link Optional}, because every one of them is honestly
 * empty somewhere: the tool window is closed, the tree is drawing a message
 * instead of a tree, nothing is selected, or the keystroke arrived in a Java
 * file that has never heard of Testin. An action asks and is disabled when the
 * answer is empty - which is the same shape {@code TreeValueUtil} already gives
 * the actions that hold their tree.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestinData {

    /**
     * The tree itself, for the actions that move around in it rather than act on
     * one node - expanding, collapsing, re-selecting after a change.
     */
    public static final @NotNull DataKey<SimpleTree> TREE = DataKey.create("testin.tree");

    /**
     * Every node the tree has selected, in selection order, and an empty list
     * when it has none. Never absent when the tree is there: "nothing selected"
     * is a state of the tree, not a missing answer.
     */
    public static final @NotNull DataKey<List<DirectoryDto>> SELECTED_NODES = DataKey.create("testin.selectedNodes");

    /**
     * The editor the keystroke arrived in - the test set editor or the run
     * editor, which answer the same questions and are asked them the same way.
     */
    public static final @NotNull DataKey<TestinEditor> EDITOR = DataKey.create("testin.editor");

    /**
     * Every test case the editor has selected, in selection order, and an empty
     * list when it has none.
     */
    public static final @NotNull DataKey<List<TestCaseDto>> SELECTED_CASES = DataKey.create("testin.selectedCases");

    /**
     * Puts this editor's answers where an action can find them.
     */
    public static void from(final @NotNull DataSink sink, final @NotNull TestinEditor editor, final @NotNull List<TestCaseDto> selected) {
        sink.set(EDITOR, editor);
        sink.set(SELECTED_CASES, selected);
    }

    /**
     * The editor the keystroke arrived in, and empty when it did not arrive in
     * one - which is how a declared editor action stays gray in a Java file.
     */
    public static @NotNull Optional<TestinEditor> editor(final @NotNull AnActionEvent e) {
        return Optional.ofNullable(EDITOR.getData(e.getDataContext()));
    }

    /**
     * Every selected test case, and none outside an editor.
     */
    public static @NotNull List<TestCaseDto> selectedCases(final @NotNull AnActionEvent e) {
        return Optional.ofNullable(SELECTED_CASES.getData(e.getDataContext())).orElse(List.of());
    }

    /**
     * The one selected test case, when exactly one is.
     */
    public static @NotNull Optional<TestCaseDto> singleSelectedCase(final @NotNull AnActionEvent e) {
        final @NotNull List<TestCaseDto> selected = selectedCases(e);

        return selected.size() == 1 ? Optional.of(selected.getFirst()) : Optional.empty();
    }

    /**
     * Puts this tree's answers where an action can find them.
     * <p>
     * Called from the surface's {@code uiDataSnapshot}, so the answers are taken
     * on the EDT while the tree is settled rather than read from a background
     * thread mid-update.
     */
    public static void from(final @NotNull DataSink sink, final @NotNull SimpleTree tree, final @NotNull List<DirectoryDto> selected) {
        sink.set(TREE, tree);
        sink.set(SELECTED_NODES, selected);
    }

    /**
     * The tree the keystroke arrived in, and empty when it did not arrive in one.
     */
    public static @NotNull Optional<SimpleTree> tree(final @NotNull AnActionEvent e) {
        return Optional.ofNullable(TREE.getData(e.getDataContext()));
    }

    /**
     * Every selected node, and none when the keystroke did not arrive in a
     * Testin surface at all.
     */
    public static @NotNull List<DirectoryDto> selectedNodes(final @NotNull AnActionEvent e) {
        return Optional.ofNullable(SELECTED_NODES.getData(e.getDataContext())).orElse(List.of());
    }

    /**
     * The one selected node, when exactly one is selected and it is of this kind.
     * <p>
     * The enablement rule almost every tree action shares, and the reason it is
     * here rather than in each of them: a change to what counts as a selection is
     * made once. It is the same question {@code TreeValueUtil.singleSelected}
     * answers for an action that holds its tree, asked of the event instead.
     */
    public static <T> @NotNull Optional<T> singleSelected(final @NotNull AnActionEvent e, final @NotNull Class<T> type) {
        final @NotNull List<DirectoryDto> selected = selectedNodes(e);

        return selected.size() == 1 ? Optional.of(selected.getFirst()).filter(type::isInstance).map(type::cast) : Optional.empty();
    }

    /**
     * The one selected node, whatever kind it is.
     */
    public static @NotNull Optional<DirectoryDto> singleSelectedNode(final @NotNull AnActionEvent e) {
        return singleSelected(e, DirectoryDto.class);
    }
}
