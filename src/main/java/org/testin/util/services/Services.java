package org.testin.util.services;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Services {

    public static <T> T getInstance(final @NotNull Project p, final @NotNull Class<T> clazz) {
        return p.getService(clazz);
    }

}
