package org.testin.importexport.exports;

import org.testin.notifications.Done;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.logger.Logger;
import org.testin.model.TestEditorAttributes;
import org.testin.model.TestEditorAttributes.Can;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.TestCaseOrder;
import org.testin.ui.dialogs.DestinationForm;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.services.BackgroundWork;
import org.testin.util.Bundle;
import org.testin.util.Mapper;

import java.io.InputStream;
import java.util.*;

/**
 * Writes what a node holds out to a file the tester can send.
 * <p>
 * Declared in {@code plugin.xml} (#119), so Find Action offers it and a tester
 * can bind a key to it - it has never had one. That is why it has no
 * constructor and no fields: the platform builds one instance for the whole IDE,
 * so the node comes from the keystroke, and the columns an export holds are the
 * attributes' own answer rather than a list this carried for three exporters to
 * read off it.
 * <p>
 * The export itself is in {@link Work}, which is what a keystroke that arrived
 * on a node with a project behind it has to work with.
 */
public class ExportAction extends DumbAwareAction {

    /** The gesture's name, which its dialog reads rather than spells. */
    public static final @NotNull String NAME = Bundle.message("export.action.name");

    // UC-SHARE-001, UC-SHARE-002
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.firstSelected(e, DirectoryDto.class).ifPresent(dir -> new Work(p).exportFrom(dir));
    }

    /**
     * UC-SHARE-001.
     * <p>
     * On a node that holds test cases, and gray on every other - including
     * outside the Testin tree altogether, which is what keeps a key bound to
     * this inert in a Java file (#119).
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TestinData.singleSelectedNode(e)
                .filter(DirectoryDto::isTestCaseContainer)
                .isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    /**
     * Exporting one node, for a project that is there.
     */
    private record Work(@NotNull Project p) {

        /**
         * UC-SHARE-001, Rule-SHARE-015.
         * <p>
         * Everything the action does once it knows which node it is exporting
         * from.
         */
        private void exportFrom(final @NotNull DirectoryDto dirDto) {
            final @NotNull Optional<VirtualFile> resolved = resolveTargetDir(dirDto);
            if (resolved.isEmpty()) return;
            final @NotNull VirtualFile targetDir = resolved.orElseThrow();

            BackgroundWork.run(p, Bundle.message("export.task.reading", dirDto.getName()), Bundle.message("export.failed.title"), gathering -> {
                final @NotNull Gathered gathered = gather(targetDir);
                final @NotNull Map<String, List<TestCaseDto>> sheets = gathered.sheets();
                if (sheets.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("notification.export.empty.title"), Bundle.message("export.none.found")));
                    return;
                }

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (gathered.unreadable().isEmpty()) {
                        chooseWhatToExport(sheets, targetDir);
                        return;
                    }

                    // Before the file is written, not after: an export missing test
                    // cases looks exactly like a whole one, and the count in the
                    // Exported message counts what was gathered, so it looks right
                    // too (#263).
                    new ConfirmDialog(p, Bundle.message("export.unreadable.title"), unreadableWarning(gathered.unreadable()),
                            "", "", Bundle.message("export.anyway"), () -> chooseWhatToExport(sheets, targetDir)).show();
                });
            });
        }

        /**
         * The framework dialog reports through this callback rather than a return
         * code, so the destination is never read back out of a dialog that was
         * canceled. It hands back the cases the tester left ticked, not the ones
         * gathered above.
         */
        private void chooseWhatToExport(final @NotNull Map<String, List<TestCaseDto>> sheets, final @NotNull VirtualFile targetDir) {
            new ExportDialog(p, TestEditorAttributes.all(Can.EXPORT), sheets, targetDir, this::writeExport).show();
        }

        /**
         * UC-SHARE-001, Rule-SHARE-005.
         * <p>
         * The write, once the tester has chosen a file. Under its own bar and after
         * the dialog has closed: a workbook of several hundred cases took the EDT
         * with it, and the dialog sat there for all of it (#87).
         */
        private void writeExport(final DestinationForm.@NotNull Destination destination, final @NotNull Map<String, List<TestCaseDto>> selected) {
            final int cases = selected.values().stream().mapToInt(List::size).sum();

            BackgroundWork.run(p, Bundle.message("export.task.writing", String.valueOf(cases), destination.file().getName()),
                    Bundle.message("export.failed.title"), indicator -> {
                        destination.format().exportToFile(p, destination.file(), selected);

                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).softShowCounted(p, Done.EXPORTED, cases));
                    });
        }

        /**
         * UC-SHARE-002, Rule-SHARE-001.
         * <p>
         * Every test set beneath the node, however deep.
         * <p>
         * It used to look one level down and no further, so exporting a package
         * whose test sets sit inside sub-packages gathered nothing from them - and
         * said nothing, so the tester sent a file missing most of what they meant to
         * send (#262).
         * <p>
         * One walk for both kinds of node. A test set holds its cases directly and
         * has no folders under it, so the recursion simply stops - which is what the
         * two branches here used to say the long way.
         */
        private @NotNull Gathered gather(final @NotNull VirtualFile targetDirectory) {
            final @NotNull List<Sheet> found = new ArrayList<>();
            final @NotNull List<String> unreadable = new ArrayList<>();

            walk(targetDirectory, List.of(targetDirectory.getName()), found, unreadable);

            final @NotNull Map<String, List<TestCaseDto>> sheets = new LinkedHashMap<>();
            for (final Sheet sheet : found) sheets.put(uniqueKey(sheets, sheet.path()), sheet.cases());

            return new Gathered(sheets, unreadable);
        }

        private void walk(final @NotNull VirtualFile dir, final @NotNull List<String> path, final @NotNull List<Sheet> found, final @NotNull List<String> unreadable) {
            final @NotNull List<TestCaseDto> here = loadTestCasesInOrder(dir, unreadable);
            if (!here.isEmpty()) found.add(new Sheet(path, here));

            for (final VirtualFile child : childrenOf(dir)) {
                if (!child.isDirectory()) continue;

                final @NotNull List<String> under = new ArrayList<>(path);
                under.add(child.getName());
                walk(child, under, found, unreadable);
            }
        }

        /**
         * UC-SHARE-002, Rule-SHARE-001.
         * <p>
         * The test cases in one folder, and the names of the files that would not
         * read. The failures used to go to the log alone, where nothing points at
         * them (#263).
         */
        private @NotNull List<TestCaseDto> loadTestCasesInOrder(final @NotNull VirtualFile dir, final @NotNull List<String> unreadable) {
            final @NotNull List<TestCaseDto> loaded = new ArrayList<>();

            for (final VirtualFile file : childrenOf(dir)) {
                if (!file.isDirectory() && file.getName().endsWith(".json")) {
                    try (InputStream is = file.getInputStream()) {
                        loaded.add(Services.getInstance(p, Mapper.class).readValue(is, TestCaseDto.class));
                    } catch (final Exception ex) {
                        Logger.error("Loading test cases failed: " + ex.getMessage());
                        unreadable.add(file.getName());
                    }
                }
            }

            // The same order the editor shows, from the same rule - a sheet whose
            // rows are in a different order from the screen they were exported from
            // is a sheet nobody trusts.
            return new ArrayList<>(TestCaseOrder.ordered(loaded));
        }
    }

    /**
     * What a walk of the exported node found: one sheet per folder holding test
     * cases, and the test case files it could not read.
     */
    private record Gathered(@NotNull Map<String, List<TestCaseDto>> sheets, @NotNull List<String> unreadable) {
    }

    /**
     * One folder that holds test cases, and where it sits under the node being
     * exported. The path is kept rather than a name because two test sets in
     * different sub-packages can share one.
     */
    private record Sheet(@NotNull List<String> path, @NotNull List<TestCaseDto> cases) {
    }

    /**
     * UC-SHARE-002, Rule-SHARE-001.
     * <p>
     * What is about to be missing, named rather than counted: a tester who
     * recognises the file knows whether the export is worth sending.
     */
    private static @NotNull String unreadableWarning(final @NotNull List<String> unreadable) {
        final @NotNull String named = String.join(", ", unreadable.subList(0, Math.min(5, unreadable.size())));
        final @NotNull String rest = unreadable.size() > 5
                ? Bundle.message("export.unreadable.more", String.valueOf(unreadable.size() - 5))
                : "";
        final @NotNull String count = unreadable.size() == 1
                ? Bundle.message("export.unreadable.one")
                : Bundle.message("export.unreadable.many", String.valueOf(unreadable.size()));

        return Bundle.message("export.unreadable.message", count, named, rest);
    }

    /**
     * The shortest tail of a test set's path that no earlier sheet has taken -
     * its own name where that is free, and its parent's name in front of it
     * where it is not.
     * <p>
     * A name rather than a number, because the tester reads these as sheet
     * titles and has to tell two same-named test sets apart by where they live.
     */
    private static @NotNull String uniqueKey(final @NotNull Map<String, ?> taken, final @NotNull List<String> path) {
        for (int from = path.size() - 1; from >= 0; from--) {
            final @NotNull String key = String.join(" - ", path.subList(from, path.size()));
            if (!taken.containsKey(key)) return key;
        }

        return String.join(" - ", path) + " (" + (taken.size() + 1) + ")";
    }

    /**
     * Empty when the path is not in the VFS; a file resolves to its parent
     * directory.
     */
    private static @NotNull Optional<VirtualFile> resolveTargetDir(final @NotNull DirectoryDto dirDto) {
        return Optional.ofNullable(LocalFileSystem.getInstance().findFileByPath(dirDto.getPath().toString()))
                .map(target -> target.isDirectory() ? target : target.getParent());
    }

    /**
     * What a VFS directory holds. The platform answers "nothing readable here"
     * with a null array, which is the same as holding nothing.
     */
    private static VirtualFile @NotNull [] childrenOf(final @NotNull VirtualFile dir) {
        return Objects.requireNonNullElse(dir.getChildren(), VirtualFile.EMPTY_ARRAY);
    }
}
