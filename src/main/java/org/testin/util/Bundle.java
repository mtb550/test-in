package org.testin.util;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

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
    public static Supplier<@Nls String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return INSTANCE.getLazyMessage(key, params);
    }

    @NotNull
    public static String getPluginName() {
        return Bundle.message("testin.display.name");
    }
}