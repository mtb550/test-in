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
@Builder
record DialogDto(@NonNull String title,
                 @NonNull List<? extends ComponentDialogBase<?>> components,
                 @NonNull List<StatusBarShortcut> shortcuts) {
}
