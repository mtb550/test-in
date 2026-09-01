package org.testin.clipboard;

import org.testin.notifications.Done;
import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.editor.TestinEditor;
import org.testin.editor.test.TestEditor;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.testcase.TestCaseSnapshot;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.ClipboardContents;
import org.testin.util.Mapper;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

public class PasteTestCaseNodeAction extends AbstractProjectAction {

    private static final @NotNull KeyStroke SHORTCUT = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
    private final @NotNull TestinEditor editor;

    public PasteTestCaseNodeAction(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Paste Node", "Paste selected test cases from clipboard", AllIcons.Actions.MenuPaste);
        this.editor = editor;
        this.registerCustomShortcutSet(Shortcuts.customShortcut(SHORTCUT), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @NotNull List<TestCaseDto> pastedCases = getFromClipboard();
        if (pastedCases.isEmpty()) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!(editor instanceof TestEditor destUI)) return;

            final @NotNull CutState cutState = Services.getInstance(p, CutState.class);
            final boolean isCut = cutState.isCutting();

            // What a CTRL+Z would have to put back on the source side, taken
            // before the cut takes it away. Empty when this is a copy, which
            // leaves the source alone and has nothing there to put back.
            final @NotNull Optional<TestCaseSnapshot> cutFrom = cutState.source().map(sourceUI -> {
                final @NotNull List<TestCaseDto> cutItems = sourceUI.getAllTestCases().stream()
                        .filter(tc -> cutState.isPending(tc.getId()))
                        .toList();

                final @NotNull TestCaseSnapshot taken = TestCaseSnapshot.of(p, sourceUI.getParent().getPath(), TestCaseSnapshot.idsOf(cutItems));

                ApplicationManager.getApplication().runWriteAction(() -> {
                    final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                    for (final TestCaseDto tc : cutItems) {
                        indexer.removeTestCase(sourceUI.getParent().getPath(), tc.getId());
                    }
                });

                sourceUI.getAllTestCases().removeAll(cutItems);
                if (sourceUI != destUI && sourceUI instanceof TestEditor sourceEditor) {
                    sourceEditor.reorderAndPersist();
                }

                return taken;
            });

            final @NotNull List<TestCaseDto> pastedHere = new ArrayList<>(pastedCases.size());

            for (final TestCaseDto tc : pastedCases) {
                final @NotNull TestCaseDto clonedTc = cloneForPasting(p, tc, isCut);

                clonedTc.setParent(destUI.getParent());
                destUI.getAllTestCases().add(clonedTc);
                pastedHere.add(clonedTc);
            }

            final int pasted = pastedHere.size();

            // Both sides of the move in one operation, so a cut here and a paste
            // there is one press of CTRL+Z rather than two (#165). Taken before
            // the sequence write puts the pasted cases on disk, which is why
            // they read as absent.
            final @NotNull Path destPath = destUI.getParent().getPath();
            final @NotNull List<UUID> pastedIds = TestCaseSnapshot.idsOf(pastedHere);
            final @NotNull List<TestCaseSnapshot> before = new ArrayList<>();
            cutFrom.ifPresent(before::add);
            before.add(TestCaseSnapshot.of(p, destPath, pastedIds));

            destUI.reorderAndPersist(() -> {
                final @NotNull List<TestCaseSnapshot> after = new ArrayList<>();
                cutFrom.ifPresent(taken -> after.add(TestCaseSnapshot.of(p, taken.testSetPath(), taken.ids())));
                after.add(TestCaseSnapshot.of(p, destPath, pastedIds));

                TestCaseSnapshot.record(p, TestCaseSnapshot.describe(isCut ? "Move" : "Paste", pastedHere), before, after, destUI::reload);
            });

            if (isCut) cutState.clear();

            // Inside the invokeLater and after the sequence is persisted: the
            // action itself returns long before the cases exist (#62).
            if (pasted > 0) Services.getInstance(p, Notifier.class).softShowCounted(p, Done.PASTED, pasted);
        });
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {

        e.getPresentation().setEnabled(ClipboardContents.withFlavor(DataFlavor.stringFlavor)
                .map(this::holdsTestCases)
                .orElse(false));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - the clipboard is BGT-safe and update() reads no Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }

    /**
     * Whether the clipboard holds test cases. Anything else on it belongs to
     * some other copy and leaves the menu entry disabled.
     */
    private boolean holdsTestCases(final @NotNull Transferable contents) {
        return !readTestCases(contents).isEmpty();
    }

    private @NotNull List<TestCaseDto> getFromClipboard() {
        return ClipboardContents.withFlavor(DataFlavor.stringFlavor)
                .map(this::readTestCases)
                .orElseGet(List::of);
    }

    /**
     * The test cases on the clipboard, or none of them. Text that is not a JSON
     * array is not a failed read - it is a tester copying a word - so it is
     * turned away before the parser sees it and reports it as a warning.
     */
    private @NotNull List<TestCaseDto> readTestCases(final @NotNull Transferable contents) {
        try {
            final @NotNull String json = (String) contents.getTransferData(DataFlavor.stringFlavor);
            if (!json.trim().startsWith("[")) return List.of();

            final @NotNull List<TestCaseDto> parsed = Services.getInstance(p, Mapper.class).readValue(json, new TypeReference<>() {
            });

            // Hand-edited JSON can carry a null entry, and the clipboard is not a
            // trusted source of our own format.
            return parsed.stream().filter(Objects::nonNull).toList();
        } catch (final Exception ex) {
            Logger.warn("[WARNING] Failed to parse clipboard JSON: " + ex.getMessage());
            return List.of();
        }
    }

    private @NotNull TestCaseDto cloneForPasting(final @NotNull Project p, final @NotNull TestCaseDto original, final boolean isCut) {
        final @NotNull ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        final @NotNull TestCaseDto clonedTc = Services.getInstance(p, Mapper.class).convertValue(original, TestCaseDto.class);

        if (isCut) {
            clonedTc.setUpdatedAt(now);
        } else {
            clonedTc.setId(UUID.randomUUID())
                    .setDescription(original.getDescription() + " (Copy)")
                    .setCreatedAt(now)
                    .setUpdatedAt(now);
        }

        return clonedTc;
    }
}
