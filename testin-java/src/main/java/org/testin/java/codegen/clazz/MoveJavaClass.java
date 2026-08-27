package org.testin.java.codegen.clazz;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.JavaSourceRoot;
import org.testin.codegen.Moved;
import org.testin.java.codegen.PackageDeclarations;
import org.testin.logger.Logger;

import java.util.Optional;
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

        final @NotNull List<String> fqcn = Fqcn.ofClass(p, moved.dir());
        if (fqcn.isEmpty()) return;

        final @NotNull Optional<List<String>> found = moved.destinationPackage(p);
        if (found.isEmpty()) {
            Logger.info("Destination is not indexed, so " + String.join(".", fqcn) + " is left where it is");
            return;
        }

        final @NotNull List<String> destination = found.get();
        final @NotNull String fileName = fqcn.getLast() + ".java";

        JavaSourceRoot.commandInRoot(p, "Move Test Class", "moving class", sourceRoot -> {
            final @NotNull Optional<VirtualFile> file =
                    Optional.ofNullable(sourceRoot.findFileByRelativePath(String.join("/", fqcn) + ".java"));

            if (file.isEmpty()) {
                Logger.info("Class not found for move: " + String.join(".", fqcn));
                return;
            }

            final @NotNull Optional<VirtualFile> target = JavaSourceRoot.packageFolder(sourceRoot, destination);

            // No folder to move into, and a class dropped where it already is:
            // neither is a move.
            if (target.isEmpty() || target.get().equals(file.get().getParent())) return;

            file.get().move(this, target.get());
            PackageDeclarations.retarget(p, sourceRoot, file.get(), target.get());

            Logger.info("Moved " + fileName + " to package: " + String.join(".", destination));
        });
    }
}
