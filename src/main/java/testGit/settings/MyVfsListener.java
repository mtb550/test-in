package testGit.settings;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;

import java.util.List;

public class MyVfsListener implements BulkFileListener {
    @Override
    public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file != null && file.isValid()) {
                if (event.getPath().contains("TestGit")) {
                    Config.setRootFolder();
                    break;
                }
            }
        }
    }

}