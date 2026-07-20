package org.testin.pojo;

import lombok.Getter;

@Getter
public enum FileTypes {
    EXCEL("Excel", ".xlsx"),
    JSON("JSON", ".json"),
    CSV("CSV", ".csv"),
    HTML("HTML", ".html");

    private final String label;
    private final String extension;

    FileTypes(final String label, final String extension) {
        this.label = label;
        this.extension = extension;
    }

    public static FileTypes fromLabel(final String label) {
        for (FileTypes f : values()) {
            if (f.label.equalsIgnoreCase(label)) {
                return f;
            }
        }
        return EXCEL;
    }
}
