package org.testin.search;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.Rows;
import org.testin.ui.framework.SelectionList;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextFieldWithSelections;
import org.testin.util.Bundle;
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
public final class GlobalSearchDialog extends AbstractFrameworkDialog<TextFieldWithSelections<Hit>> {

    // UC-INTERNAL-001
    public GlobalSearchDialog(final @NotNull Project p) {
        super(p);

        title = Bundle.message("dialog.search.title");

        components = List.of(
                ComponentDialogBase.<Hit>textFieldWithSelections()
                        .icon(AllIcons.Actions.Search)
                        .placeholder(Bundle.message("dialog.search.placeholder"))
                        .rows(query -> rowsFor(p, query))
                        .build());

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, Bundle.message("dialog.search.shortcut.goto"), this::submit),
                StatusBarShortcut.select(),
                StatusBarShortcut.cancel(this::closeCancel)
        );

        preferredSize = JBUI.size(700, 460);

        // Nothing is typed into this that is worth keeping: it holds a query,
        // not an answer. A tester who clicks somewhere else has gone there
        // instead, and the dialog should not be waiting behind it
        // (Rule-INTERNAL-076).
        dismissOnClickOutside = true;
    }

    /**
     * UC-INTERNAL-001, Rule-INTERNAL-072, Rule-INTERNAL-073.
     * <p>
     * One row per hit - its icon, its name, and underneath it where it lives -
     * and beside the field, how many matched.
     * <p>
     * The icon says which kind of thing the row is, and nothing else does: it
     * is the icon the tree draws for that node, so a test set looks in the
     * search as it looks in the tree.
     * <p>
     * Where it lives is on the row because a description does not say which
     * test set it came from, and three cases can be called the same thing in
     * three different sets. The count is beside the field because the list
     * stops at fifty: a common word matches hundreds and shows fifty, and the
     * number is the only thing that says so.
     */
    private static @NotNull Rows.Answer<Hit> rowsFor(final @NotNull Project p, final @NotNull String query) {
        final @NotNull Hits.Found found = Hits.forQuery(p, query);

        final @NotNull List<SelectionList<Hit>> rows = found.hits().stream()
                .map(hit -> SelectionList.add(hit.icon(), hit.name(), hit.where(), hit))
                .toList();

        // Nothing to say before anything is typed: the field is showing its
        // placeholder, and "everywhere you can go, 214 of them" is a number
        // about the project rather than about a search.
        //
        // Nothing to say when nothing matched either - the list draws its own
        // "Nothing to show", and "0 found" above it is the same news twice.
        if (query.isBlank() || found.matched() == 0) return Rows.Answer.of(rows);

        return new Rows.Answer<>(rows, Bundle.message("dialog.search.found", found.matched()));
    }

    // UC-INTERNAL-001, Rule-INTERNAL-002
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
