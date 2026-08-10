package org.testin.util;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public final class Bundle extends DynamicBundle {
    private static final String BUNDLE = "messages";

    private static final Bundle INSTANCE = new Bundle();

    private Bundle() {
        super(Bundle.class, BUNDLE);
    }

    @NotNull
    public static @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    @NotNull
    public static String getPluginName() {
        return Bundle.message("testin.display.name");
    }
}