package testGit.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;

import java.util.Stack;

public class ActionHistory {
    private static final Stack<ActionPair> undoStack = new Stack<>();
    private static final Stack<ActionPair> redoStack = new Stack<>();

    public static void register(Runnable undo, Runnable redo) {
        undoStack.push(new ActionPair(undo, redo));
        redoStack.clear();
    }

    public static void undo() {
        if (!undoStack.isEmpty()) {
            ActionPair action = undoStack.pop();
            action.undo.run();
            redoStack.push(action); // correct: keep both undo/redo
        }
    }

    public static void redo() {
        if (!redoStack.isEmpty()) {
            ActionPair action = redoStack.pop();
            action.redo.run();
            undoStack.push(action); // correct: re-push for further undos
        }
    }

    public static void showStatus(Project project) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar != null) {
            statusBar.setInfo("Undo: " + undoStack.size() + " | Redo: " + redoStack.size());
        }

        StatusUtil.showStatus(project, "Undo: " + undoStack.size() + " | Redo: " + redoStack.size());

    }

    private static class ActionPair {
        public final Runnable undo;
        public final Runnable redo;

        public ActionPair(Runnable undo, Runnable redo) {
            this.undo = undo;
            this.redo = redo;
        }
    }

}
