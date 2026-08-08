package org.testin.enums;

import com.intellij.openapi.vfs.VirtualFile;

@FunctionalInterface
public interface IVfsOperation {
    void execute(VirtualFile vf);
}
