package testGit.editorPanel;

import testGit.pojo.TestCase;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class Model extends AbstractTableModel {
    private final String[] columnNames = {"#", "Title", "Expected Result", "Steps", "Priority", "Automation Ref"};
    private final List<TestCase> testCases;

    public Model(List<TestCase> testCases) {
        System.out.println("EditorModel.EditorModel()");
        this.testCases = testCases;
    }

    @Override
    public int getRowCount() {
        System.out.println("EditorModel.getRowCount()");
        return testCases.size();
    }

    @Override
    public int getColumnCount() {
        System.out.println("EditorModel.getColumnCount()");
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        System.out.println("EditorModel.getColumnName()");
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        System.out.println("EditorModel.getValueAt()");
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
        System.out.println("EditorModel.getTestCaseAt()");
        return testCases.get(rowIndex);
    }
}
