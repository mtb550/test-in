package testGit.ui.single;

import com.intellij.openapi.ui.popup.JBPopup;
import testGit.pojo.dto.TestCaseDto;
import testGit.ui.single.nnew.BaseCreateTestCase;

import java.util.function.Consumer;

public class SingleEditorSaveManager {

    public static Runnable createSaveAction(
            BaseCreateTestCase form,
            TestCaseDto dto,
            Consumer<TestCaseDto> onSave,
            JBPopup[] popupWrapper) {

        return () -> {
            form.getTitleSection().applyTo(dto);
            form.getExpectedSection().applyTo(dto);
            form.getPrioritySection().applyTo(dto);
            form.getGroupsSection().applyTo(dto);
            form.getStepsSection().applyTo(dto);

            String title = dto.getTitle();
            if (form.getTitleSection().getWrapper().getParent() == null || (title != null && !title.isEmpty())) {
                onSave.accept(dto);
                popupWrapper[0].closeOk(null);
            }
        };
    }
}