package org.testin.editor.listeners;

import lombok.AllArgsConstructor;
import org.testin.notifications.Done;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editor.TestinEditor;
import org.testin.testcase.TestCaseSnapshot;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
public class TransferListener extends TransferHandler {
    private static final @NotNull DataFlavor FLAVOR = new DataFlavor(List.class, "List of TestCase");
    private final @NotNull Project p;
    private final @NotNull TestinEditor editor;


    // UC-EDITOR-PANEL-010, Rule-EDITOR-PANEL-058
    @Override
    public int getSourceActions(final JComponent c) {
        return MOVE;
    }

    @Override
    protected @Nullable Transferable createTransferable(final JComponent c) {
        if (!(c instanceof JBList<?> rawList)) return null;

        final @NotNull List<TestCaseDto> items = rawList.getSelectedValuesList().stream()
                .filter(TestCaseDto.class::isInstance)
                .map(TestCaseDto.class::cast)
                .toList();

        return new Transferable() {
            @Override
            public DataFlavor @NotNull [] getTransferDataFlavors() {
                return new DataFlavor[]{FLAVOR};
            }

            @Override
            public boolean isDataFlavorSupported(final DataFlavor flavor) {
                return FLAVOR.equals(flavor);
            }

            @Override
            public @NotNull Object getTransferData(final DataFlavor flavor) {
                return items;
            }
        };
    }

    @Override
    public boolean canImport(final TransferSupport support) {
        return support.isDataFlavorSupported(FLAVOR);
    }

    // UC-EDITOR-PANEL-010, Rule-EDITOR-PANEL-061
    @Override
    public boolean importData(final TransferSupport support) {
        // Asked before the transferable is, because getTransferData throws for a
        // flavor it does not carry - and the exception's message is the flavor's
        // own name, so the log read "Exception: List of TestCase" and said
        // nothing about a drag that was simply not ours to take.
        if (!support.isDataFlavorSupported(FLAVOR)) return false;

        // Before the try, because the try rearranges it: the master list is the
        // one thing here that is changed in memory and persisted afterward, so a
        // failure between the two has to be able to put it back.
        final @NotNull List<TestCaseDto> orderBefore = editor.snapshotOfAll();

        try {
            final @NotNull Object data = support.getTransferable().getTransferData(FLAVOR);
            if (!(data instanceof List<?> rawList)) return false;

            final @NotNull List<TestCaseDto> itemsToMove = rawList.stream()
                    .filter(TestCaseDto.class::isInstance)
                    .map(TestCaseDto.class::cast)
                    .toList();

            if (itemsToMove.isEmpty()) return false;

            final @NotNull Set<UUID> movedIds = itemsToMove.stream().map(TestCaseDto::getId).collect(Collectors.toSet());

            // Read before the removal, because it is found among the visible
            // rows and those still hold the dragged cases.
            final @NotNull Optional<TestCaseDto> above = anchorAboveDrop(support, movedIds);
            final @NotNull Optional<TestCaseDto> below = anchorBelowDrop(support, movedIds);

            final @NotNull List<TestCaseDto> allItems = editor.getAllTestCases();
            final @NotNull List<UUID> ids;

            synchronized (allItems) {
                // A drag that did not start on this list: there is nothing here
                // to reorder, and treating it as an insert would duplicate.
                final @NotNull Set<UUID> here = allItems.stream().map(TestCaseDto::getId).collect(Collectors.toSet());
                if (!here.containsAll(movedIds)) return false;

                allItems.removeIf(tc -> movedIds.contains(tc.getId()));
                allItems.addAll(landingIndex(allItems, above, below), itemsToMove);
                ids = TestCaseSnapshot.idsOf(allItems);
            }

            // Every case in the set, not only the ones dragged: moving one case
            // past three others rewrites the rank of all four, so all four are
            // what putting the drag back has to restore.
            //
            // Taken before the write and again from its callback, because the
            // write is asynchronous - a snapshot taken after the call returns
            // would still be the order before the drag.
            final @NotNull Path setPath = editor.getParent().getPath();
            final @NotNull TestCaseSnapshot before = TestCaseSnapshot.of(p, setPath, ids);

            editor.updateSequenceAndSaveAll(() -> TestCaseSnapshot.record(p, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.reorder"), itemsToMove), before, TestCaseSnapshot.of(p, setPath, ids)));

            // After the save, inside the try: a drop that threw on the way here
            // is logged, not confirmed (#62).
            Services.getInstance(p, Notifier.class).softShowCounted(p, Done.RE_SORTED, itemsToMove.size());

            itemsToMove.stream().findFirst().ifPresentOrElse(
                    editor::selectTestCase,
                    editor::refreshView
            );

            return true;
        } catch (final Exception ex) {
            // The list was rearranged inside the try and the save comes after it,
            // so a throw between the two left the editor holding an order nothing
            // had written - and said nothing at all, to anybody but the log. The
            // order goes back and the tester is told (#66, finding 81).
            putBack(orderBefore);

            Logger.error("Reordering the test cases failed: " + ex.getMessage());
            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("reorder.failed"));
            return false;
        }
    }

    /**
     * UC-EDITOR-PANEL-015.
     * <p>
     * The order the editor had before the drop, put back and redrawn.
     */
    private void putBack(final @NotNull List<TestCaseDto> orderBefore) {
        final @NotNull List<TestCaseDto> allItems = editor.getAllTestCases();

        synchronized (allItems) {
            allItems.clear();
            allItems.addAll(orderBefore);
        }

        editor.refreshView();
    }

    /**
     * The case the drop landed above: the first visible row at or after the drop
     * point that is not itself being dragged, and empty when the drop was past
     * the last of them.
     * <p>
     * A case rather than a row number, because the two are not the same list. A
     * drop location counts rows on screen - one page of whatever the filter left
     * - while the list being reordered is the whole test set. Under a filter the
     * two index spaces differ, and the row number was applied to the full list
     * anyway, so a drag moved cases the tester never touched and saved them
     * (#163).
     * <p>
     * Kept as the fallback for a drop with nothing visible above it - the top of
     * a page - where inserting after the card above is not a position this page
     * can name. Without it a card dropped at the top of page two went to the top
     * of the whole test set.
     */
    private @NotNull Optional<TestCaseDto> anchorBelowDrop(final @NotNull TransferSupport support, final @NotNull Set<UUID> movedIds) {
        if (!(support.getComponent() instanceof JBList<?> target)) return Optional.empty();

        final @NotNull ListModel<?> rows = target.getModel();

        for (int row = Math.max(0, ((JBList.DropLocation) support.getDropLocation()).getIndex()); row < rows.getSize(); row++) {
            if (rows.getElementAt(row) instanceof TestCaseDto tc && !movedIds.contains(tc.getId())) return Optional.of(tc);
        }

        return Optional.empty();
    }

    /**
     * The visible case the drop landed under: the last row before the drop point
     * that is not itself being dragged, and empty when the drop was above all of
     * them.
     * <p>
     * The card above rather than the card below, which is what decides where the
     * hidden cases end up. Anchoring below meant "before the next visible card",
     * so a case dropped between two visible cards landed after every case the
     * filter was hiding between them - saved, confirmed as Re-sorted, and
     * nowhere the tester could see it (#209). Anchoring above puts it straight
     * after the card it was dropped under, which is the one position the tester
     * can actually point at.
     */
    // UC-EDITOR-PANEL-010, Rule-EDITOR-PANEL-059
    private @NotNull Optional<TestCaseDto> anchorAboveDrop(final @NotNull TransferSupport support, final @NotNull Set<UUID> movedIds) {
        if (!(support.getComponent() instanceof JBList<?> target)) return Optional.empty();

        final @NotNull ListModel<?> rows = target.getModel();
        final int drop = Math.min(((JBList.DropLocation) support.getDropLocation()).getIndex(), rows.getSize());

        for (int row = drop - 1; row >= 0; row--) {
            if (rows.getElementAt(row) instanceof TestCaseDto tc && !movedIds.contains(tc.getId())) return Optional.of(tc);
        }

        return Optional.empty();
    }

    /**
     * Where the dragged cases go in the whole test set.
     * <p>
     * After the card the drop landed under, and before the card it landed above
     * when there is nothing under it - the top of a page, where "after the card
     * above" names no position this page can see. With neither, the page holds
     * nothing visible and the cases go to the end.
     */
    private static int landingIndex(final @NotNull List<TestCaseDto> allItems, final @NotNull Optional<TestCaseDto> above, final @NotNull Optional<TestCaseDto> below) {
        return above.map(tc -> Math.min(indexOfId(allItems, tc.getId()) + 1, allItems.size()))
                .or(() -> below.map(tc -> indexOfId(allItems, tc.getId())))
                .orElse(allItems.size());
    }

    /**
     * Where that case sits in the list being reordered, and the end of the list
     * when it is not there.
     * <p>
     * By id rather than by object: a reload hands back new instances for the
     * same test cases, and one can land between the drag starting and the drop
     * arriving.
     */
    private static int indexOfId(final @NotNull List<TestCaseDto> items, final @NotNull UUID id) {
        for (int i = 0; i < items.size(); i++) {
            if (id.equals(items.get(i).getId())) return i;
        }

        return items.size();
    }
}