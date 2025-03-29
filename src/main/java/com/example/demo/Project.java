package com.example.demo;

import java.util.List;

public class Project {
    private final String name;
    private final List<Feature> features;

    public Project(String name, List<Feature> features) {
        this.name = name;
        this.features = features;
    }

    public String getName() {
        return name;
    }

    public List<Feature> getFeatures() {
        return features;
    }
}
