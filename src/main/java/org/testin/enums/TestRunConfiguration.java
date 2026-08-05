package org.testin.enums;

import com.intellij.icons.AllIcons;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.swing.*;

@Getter
@AllArgsConstructor
public enum TestRunConfiguration {

    TEST_TYPE(
            "Test Type",
            AllIcons.Nodes.Type,
            new String[]{"", "Functional Test", "Performance Test"}
    ),

    RELEASE_NOTES(
            "Release Notes",
            AllIcons.Nodes.Type,
            null
    ),

    COMMIT_ID(
            "Commit ID",
            AllIcons.Nodes.Type,
            null
    ),

    PLATFORM(
            "Platform",
            AllIcons.Nodes.PpLib,
            new String[]{"", "Web", "Mobile"}
    ),

    COMPONENT(
            "Component",
            AllIcons.Nodes.PpLib,
            new String[]{"", "Frontend", "Backend"}
    ),

    LANGUAGE(
            "Language",
            AllIcons.Nodes.Lambda,
            new String[]{"", "English", "Arabic", "French"}
    ),

    BROWSER(
            "Browser",
            AllIcons.Nodes.WebFolder,
            new String[]{"", "Chrome", "Firefox", "Safari", "Edge"}
    ),

    DEVICE_TYPE(
            "Device Type",
            AllIcons.Nodes.Include,
            new String[]{"", "Desktop", "Mobile", "Tablet"}
    );

    private final String displayName;
    private final Icon icon;
    private final String[] options;
}