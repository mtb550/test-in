package org.testin.ui.framework;

import lombok.Builder;
import lombok.NonNull;

import java.util.List;

/**
 * Everything a framework dialog declares, as one object. Framework-internal:
 * {@code AbstractFrameworkDialog} packages its declaration fields here on
 * first show. All parts are {@code @NonNull}, so a field the dialog forgot to
 * assign fails immediately with a clear message.
 */
// The canonical constructor is reported as never used, and kept: Lombok's
// generated builder is what calls it, and the inspection does not see generated
// code (#61).
@Builder
record DialogDto(@NonNull String title, @NonNull List<? extends ComponentDialogBase<?>> components, @NonNull List<StatusBarShortcut> shortcuts) {
}
