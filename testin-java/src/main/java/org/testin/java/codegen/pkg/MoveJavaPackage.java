package org.testin.java.codegen.pkg;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
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
 * Moves a package's generated folder into the package its new place in the tree
 * stands for, carrying everything under it.
 * <p>
 * The folder moves and then every {@code .java} file beneath it is made to
 * declare where it now sits - including the ones in packages nested inside,
 * which move with their parent and each land somewhere new.
 */
public class MoveJavaPackage implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof Moved moved)) return;

        final @NotNull List<String> fqcn = Fqcn.ofPackage(moved.dir());
        final @NotNull List<String> destination = moved.destinationPackage(p);

        JavaSourceRoot.commandInRoot(p, "Move Test Package", "moving package", sourceRoot -> {
            final @NotNull Optional<VirtualFile> found = Optional.ofNullable(sourceRoot.findFileByRelativePath(String.join("/", fqcn)))
                    .filter(VirtualFile::isDirectory);

            if (found.isEmpty()) {
                Logger.info("Package not found for move: " + String.join(".", fqcn));
                return;
            }

            final @NotNull VirtualFile folder = found.get();
            final @NotNull Optional<VirtualFile> target = JavaSourceRoot.packageFolder(sourceRoot, destination);

            // No folder to move into; a package dropped where it already is; and a
            // package dropped into itself, which would take the folder with it.
            // None of the three is a move.
            if (target.isEmpty()
                    || target.get().equals(folder.getParent())
                    || VfsUtil.isAncestor(folder, target.get(), false)) return;

            folder.move(this, target.get());
            PackageDeclarations.retarget(p, sourceRoot, folder);

            Logger.info("Moved package " + folder.getName() + " into: " + String.join(".", destination));
        });
    }
}
