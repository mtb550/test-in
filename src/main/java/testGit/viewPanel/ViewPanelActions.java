package testGit.viewPanel;

import com.intellij.openapi.actionSystem.AnAction;
import testGit.actions.NextTestCase;
import testGit.actions.PreviousTestCase;

import javax.swing.*;
import java.util.List;

public class ViewPanelActions {

    public static List<AnAction> create(final ViewPagination page, final JComponent component) {
        return List.of(
                new PreviousTestCase(page, component),
                new NextTestCase(page, component)
        );
    }
}
