package org.testin.pojo;

import lombok.Getter;

@Getter
public enum FileTypes {
    XLS("XLS", ".xls", null),
    XLSX("XLSX", ".xlsx", """
            To ensure a successful import, your Excel file should contain the following column headers (case-insensitive):
            
            %s
            
            Note: Missing columns will safely default to empty values.
            You can also download a ready-to-use sample file using the button below."""),
    JSON("JSON", ".json", null),
    CSV("CSV", ".csv", """
            To ensure a successful import, your CSV file should contain the following column headers (case-insensitive):
            
            %s
            
            Note: Missing columns will safely default to empty values.
            The CSV should use comma as delimiter. Values containing commas or newlines must be quoted with double quotes."""),
    HTML("HTML", ".html", null);

    private final String label;
    private final String extension;
    private final String infoMessage;

    FileTypes(final String label, final String extension, final String infoMessage) {
        this.label = label;
        this.extension = extension;
        this.infoMessage = infoMessage;
    }

    public static FileTypes fromLabel(final String label) {
        for (FileTypes f : values()) {
            if (f.label.equalsIgnoreCase(label)) {
                return f;
            }
        }
        return XLSX;
    }

    public String getInfoMessage(final String columnNames) {
        if (infoMessage == null) return null;
        return String.format(infoMessage, columnNames);
    }
}
