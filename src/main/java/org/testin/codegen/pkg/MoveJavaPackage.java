package org.testin.codegen.pkg;

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

        final List<String> fqcn = Fqcn.ofPackage(moved.dir());
        final List<String> destination = moved.destinationPackage(p);

        JavaSourceRoot.writeInRoot(p, "moving package", sourceRoot -> {
            final VirtualFile folder = sourceRoot.findFileByRelativePath(String.join("/", fqcn));

            if (folder == null || !folder.isDirectory()) {
                Logger.info("Package not found for move: " + String.join(".", fqcn));
                return;
            }

            final VirtualFile target = VfsUtil.createDirectoryIfMissing(sourceRoot, String.join("/", destination));
            if (target == null) {
                Logger.error("Could not create the package to move " + folder.getName() + " into: " + String.join(".", destination));
                return;
            }

            // A package dropped where it already is, and a package dropped into
            // itself: neither is a move, and the second would take the folder
            // with it.
            if (target.equals(folder.getParent()) || VfsUtil.isAncestor(folder, target, false)) return;

            folder.move(this, target);
            PackageDeclarations.retarget(p, sourceRoot, folder);

            Logger.info("Moved package " + folder.getName() + " into: " + String.join(".", destination));
        });
    }
}
