package testGit.pojo;

import com.intellij.icons.AllIcons;
import lombok.Getter;

import javax.swing.*;

@Getter
public enum DirectoryIcon {
    PR(AllIcons.Nodes.Project),
    TCP(AllIcons.Nodes.Bookmark),
    TRP(AllIcons.Nodes.Bookmark),
    PA(AllIcons.Nodes.WebFolder),
    //TS(AllIcons.Nodes.Class),
    TS(AllIcons.FileTypes.Text),
    TR(AllIcons.Nodes.Services),
    DEFAULT(AllIcons.Nodes.Folder);

    private final Icon value;

    DirectoryIcon(Icon value) {
        this.value = value;
    }
}
