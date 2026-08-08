package org.testin.enums;

import com.intellij.openapi.vfs.VirtualFile;

@FunctionalInterface
public interface IVfsBiOperation {
    void execute(VirtualFile sourceVf, VirtualFile targetVf);
}
