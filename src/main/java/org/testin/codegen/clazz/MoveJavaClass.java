package org.testin.codegen.clazz;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.JavaSourceRoot;
import org.testin.codegen.Moved;
import org.testin.codegen.PackageDeclarations;
import org.testin.logger.Logger;

import java.util.List;

/**
 * Moves a test set's generated class into the package its new place in the tree
 * stands for.
 * <p>
 * A move is not a rename: the class keeps its name and changes the package it
 * declares. Nothing did this before, so dragging a test set moved the node and
 * left the class where it was - and because a fully-qualified name is built from
 * the tree path, every case under it stopped being runnable (#51).
 */
public class MoveJavaClass implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof Moved moved)) return;

        final List<String> fqcn = Fqcn.ofClass(p, moved.dir());
        if (fqcn.isEmpty()) return;

        final List<String> destination = moved.destinationPackage(p);
        final String fileName = fqcn.getLast() + ".java";

        JavaSourceRoot.commandInRoot(p, "Move Test Class", "moving class", sourceRoot -> {
            final VirtualFile file = sourceRoot.findFileByRelativePath(String.join("/", fqcn) + ".java");

            if (file == null) {
                Logger.info("Class not found for move: " + String.join(".", fqcn));
                return;
            }

            final VirtualFile target = VfsUtil.createDirectoryIfMissing(sourceRoot, String.join("/", destination));
            if (target == null) {
                Logger.error("Could not create the package to move " + fileName + " into: " + String.join(".", destination));
                return;
            }

            if (target.equals(file.getParent())) return;

            file.move(this, target);
            PackageDeclarations.retarget(p, sourceRoot, file, target);

            Logger.info("Moved " + fileName + " to package: " + String.join(".", destination));
        });
    }
}
