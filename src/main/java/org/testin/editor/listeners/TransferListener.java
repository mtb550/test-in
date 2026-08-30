package org.testin.editor.listeners;

import org.testin.notifications.Done;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editor.TestinEditor;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TransferListener extends TransferHandler {
    private static final @NotNull DataFlavor FLAVOR = new DataFlavor(List.class, "List of TestCase");
    private final @NotNull Project p;
    private final @NotNull TestinEditor editor;

    public TransferListener(final @NotNull Project p, final @NotNull TestinEditor editor) {
        this.p = p;
        this.editor = editor;
    }

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

    @Override
    public boolean importData(final TransferSupport support) {
        // Asked before the transferable is, because getTransferData throws for a
        // flavor it does not carry - and the exception's message is the flavor's
        // own name, so the log read "Exception: List of TestCase" and said
        // nothing about a drag that was simply not ours to take.
        if (!support.isDataFlavorSupported(FLAVOR)) return false;

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
            final @NotNull Optional<TestCaseDto> anchor = anchorBelowDrop(support, movedIds);

            final @NotNull List<TestCaseDto> allItems = editor.getAllTestCases();

            synchronized (allItems) {
                // A drag that did not start on this list: there is nothing here
                // to reorder, and treating it as an insert would duplicate.
                final @NotNull Set<UUID> here = allItems.stream().map(TestCaseDto::getId).collect(Collectors.toSet());
                if (!here.containsAll(movedIds)) return false;

                allItems.removeIf(tc -> movedIds.contains(tc.getId()));
                allItems.addAll(anchor.map(tc -> indexOfId(allItems, tc.getId())).orElse(allItems.size()), itemsToMove);
            }

            editor.updateSequenceAndSaveAll();

            // After the save, inside the try: a drop that threw on the way here
            // is logged, not confirmed (#62).
            Services.getInstance(p, Notifier.class).softShowCounted(p, Done.RE_SORTED, itemsToMove.size());

            itemsToMove.stream().findFirst().ifPresentOrElse(
                    editor::selectTestCase,
                    editor::refreshView
            );

            return true;
        } catch (final Exception ex) {
            Logger.error("Reordering the test cases failed: " + ex.getMessage());
            return false;
        }
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
     * Above rather than below: "insert before row N" is what a drop location
     * already means, so the only boundary is the one Swing itself defines, past
     * the last row appends. A case dropped between two visible cards therefore
     * lands after whatever the filter is hiding between them.
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