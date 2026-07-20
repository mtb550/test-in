package org.testin.actions.export;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.FileTypes;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.util.Mapper;
import org.testin.util.Tools;
import org.testin.util.services.Services;

import javax.swing.*;
import java.io.File;
import java.io.InputStream;
import java.util.*;

// todo: add abstract method to be override in all extends classes: exportToFile()
public abstract class ExportBase extends DumbAwareAction {
    final @NotNull SimpleTree tree;

    final List<TestEditorAttributes> EXPORT_COLUMNS = Arrays.stream(TestEditorAttributes.values())
            .filter(TestEditorAttributes::isExportable)
            .toList();

    public ExportBase(final @NotNull SimpleTree tree, final @NotNull String text, final @NotNull String description, final @NotNull Icon icon) {
        super(text, description, icon);
        this.tree = tree;
    }

    public Map<String, List<TestCaseDto>> gatherData(final @NotNull Project project, final VirtualFile targetDirectory, final DirectoryDto dirDto) {
        Map<String, List<TestCaseDto>> allSheets = new LinkedHashMap<>();

        if (dirDto instanceof TestSetDirectoryDto) {
            allSheets.put(targetDirectory.getName(), loadTestCasesInOrder(project, targetDirectory));
        } else {
            VirtualFile[] children = targetDirectory.getChildren();
            if (children != null) {
                for (VirtualFile child : children) {
                    if (child.isDirectory()) {
                        List<TestCaseDto> tcs = loadTestCasesInOrder(project, child);
                        if (!tcs.isEmpty()) {
                            allSheets.put(child.getName(), tcs);
                        }
                    }
                }
            }
        }
        return allSheets;
    }

    // todo: remove static
    public static NotificationAction createOpenAction(final @NotNull Project project, final File destFile, final String format) {
        FileTypes ef = FileTypes.fromLabel(format);
        if (ef == FileTypes.HTML) {
            return NotificationAction.createSimple("Open file", () ->
                    BrowserUtil.browse(destFile.toURI().toString())
            );
        }
        return NotificationAction.createSimple("Open file", () -> {
            VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(destFile.getAbsolutePath());
            if (vf != null) {
                Services.getInstance(project, Tools.class).openWithAssociatedProgram(project, vf);
            }
        });
    }

    public VirtualFile resolveTargetDir(final DirectoryDto dirDto) {
        VirtualFile target = LocalFileSystem.getInstance().findFileByPath(dirDto.getPath().toString());
        if (target != null && !target.isDirectory()) {
            target = target.getParent();
        }
        return target;
    }

    public List<TestCaseDto> loadTestCasesInOrder(final @NotNull Project project, final VirtualFile dir) {
        Map<UUID, TestCaseDto> tcMap = new HashMap<>();
        TestCaseDto head = null;

        VirtualFile[] files = dir.getChildren();
        if (files == null) return Collections.emptyList();

        for (VirtualFile file : files) {
            if (!file.isDirectory() && file.getName().endsWith(".json")) {
                try (InputStream is = file.getInputStream()) {
                    TestCaseDto tc = Services.getInstance(project, Mapper.class).readValue(is, TestCaseDto.class);
                    if (tc != null) {
                        tcMap.put(tc.getId(), tc);
                        if (Boolean.TRUE.equals(tc.getIsHead())) {
                            head = tc;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (head == null && !tcMap.isEmpty()) {
            return new ArrayList<>(tcMap.values());
        }

        List<TestCaseDto> orderedList = new ArrayList<>();
        TestCaseDto current = head;

        while (current != null) {
            orderedList.add(current);
            if (current.getNext() != null) {
                current = tcMap.get(current.getNext());
            } else {
                current = null;
            }
        }

        return orderedList;
    }
}
