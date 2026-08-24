package org.testin.search;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.SelectionList;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextFieldWithSelections;
import org.testin.util.Shortcuts;

import java.util.List;

/**
 * Find anything in the test project, and go to it (#29).
 * <p>
 * A field, and under it what matches: type part of a description, a step, an id,
 * a module, a reference, or the name of a test set, a package or a run. Arrow
 * down, Enter, and the tree is expanded to it with its editor open.
 * <p>
 * With nothing typed it lists every test set and every test run, which makes it
 * a way of getting around rather than only a way of finding: open it, arrow
 * down, Enter, and a set is open without going to the tree at all.
 * <p>
 * The same component the create dialogs are built from, which is what makes this
 * a declaration rather than a screen: rows that answer to the query instead of
 * rows fixed at construction, and everything else - the icon following the
 * selection, Up and Down moving it while focus stays in the field, a click
 * submitting - is already how that component behaves.
 * <p>
 * Sized rather than packed, so it does not resize as results come and go.
 */
public final class SearchDialog extends AbstractFrameworkDialog<TextFieldWithSelections<Hit>> {

    public SearchDialog(final @NotNull Project p) {
        super(p);

        title = "Search Test Project";

        components = List.of(
                ComponentDialogBase.<Hit>textFieldWithSelections()
                        .icon(AllIcons.Actions.Search)
                        .placeholder("Go to a test set or run, or search for anything...")
                        .rows(query -> rowsFor(p, query))
                        .build());

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, "Go To", this::submit),
                StatusBarShortcut.hint("↑ ↓", "Select"),
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel)
        );

        preferredSize = JBUI.size(700, 460);
    }

    /**
     * One row per hit: what it is, and underneath it where it lives - because a
     * description on its own does not say which test set it came from, and three
     * cases can be called the same thing in three different sets.
     */
    private static @NotNull List<SelectionList<Hit>> rowsFor(final @NotNull Project p, final @NotNull String query) {
        return Hits.forQuery(p, query).stream()
                .map(hit -> SelectionList.add(hit.icon(), hit.name(), hit.where(), hit))
                .toList();
    }

    @Override
    protected void submit() {
        // Empty when a query matched nothing, which is an ordinary thing for a
        // search: the dialog stays open so the tester can keep typing rather
        // than closing on them for having mistyped.
        component().selection().ifPresent(hit -> {
            closeOk();
            GoTo.the(p, hit);
        });
    }
}
