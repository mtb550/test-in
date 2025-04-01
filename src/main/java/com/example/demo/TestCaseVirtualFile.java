package com.example.demo;

import com.example.pojo.Feature;
import com.intellij.testFramework.LightVirtualFile;

import java.util.Objects;

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

    public String getProjectName() {
        return projectName;
    }

    @Override
    public String getName() {
        return "[" + projectName + "] " + feature.getName();
    }

    // ✅ Prevent duplicate tab logic
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestCaseVirtualFile that)) return false;
        return Objects.equals(projectName, that.projectName)
                && Objects.equals(feature.getName(), that.feature.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectName, feature.getName());
    }
}
