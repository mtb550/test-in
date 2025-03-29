package com.example.demo;

import com.intellij.testFramework.LightVirtualFile;

public class TestCaseVirtualFile extends LightVirtualFile {
    private final String projectName;
    private final Feature feature;

    public TestCaseVirtualFile(String projectName, Feature feature) {
        super("[" + projectName + "] " + feature.getName(), TestCaseFileType.INSTANCE, "");
        this.projectName = projectName;
        this.feature = feature;
    }

    public Feature getFeature() {
        return feature;
    }
}
