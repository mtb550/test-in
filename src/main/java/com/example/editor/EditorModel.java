package com.example.editor;

import com.example.pojo.TestCase;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class EditorModel extends AbstractTableModel {
    private final String[] columnNames = {"#", "Title", "Expected Result", "Steps", "Priority", "Automation Ref"};
    private final List<TestCase> testCases;

    public EditorModel(List<TestCase> testCases) {
        this.testCases = testCases;
    }

    @Override
    public int getRowCount() {
        return testCases.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        TestCase tc = testCases.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> rowIndex + 1;
            case 1 -> tc.getTitle();
            case 2 -> tc.getExpectedResult();
            case 3 -> tc.getSteps();
            case 4 -> tc.getPriority();
            case 5 -> tc.getAutomationRef();
            default -> throw new Error("Invalid column index: " + columnIndex + ". name: " + columnNames[columnIndex]);
        };
    }

    public TestCase getTestCaseAt(int rowIndex) {
        return testCases.get(rowIndex);
    }
}
