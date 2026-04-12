package testGit.pojo;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.swing.*;

@Getter
@AllArgsConstructor
public enum RunMetadataFields {
    BUILD_NUMBER("Build Number",
            AllIcons.Actions.Edit,
            null
    ),

    PLATFORM(
            "Platform",
            AllIcons.Nodes.PpLib, new
            String[]{"Windows", "Linux", "MacOS", "Android", "iOS"}
    ),

    LANGUAGE(
            "Language",
            AllIcons.Nodes.Lambda, new
            String[]{"English", "Arabic", "French"}
    ),

    BROWSER(
            "Browser",
            AllIcons.Nodes.WebFolder, new
            String[]{"Chrome", "Firefox", "Safari", "Edge"}
    ),

    DEVICE_TYPE(
            "Device Type",
            AllIcons.Nodes.Include,
            new String[]{"Desktop", "Mobile", "Tablet"}
    );

    private final String displayName;
    private final Icon icon;
    private final String[] options;
}