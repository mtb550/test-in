/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.ui.framework;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.ui.popup.PopupFactoryImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

public final class SizedPopups extends PopupFactoryImpl {

    public static void installed(final @NotNull Disposable owner) {
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), JBPopupFactory.class, new SizedPopups(), owner);
    }

    @Override
    public @NotNull ComponentPopupBuilder createComponentPopupBuilder(final @NotNull JComponent content, final @Nullable JComponent preferableFocusComponent) {
        final @NotNull ComponentPopupBuilder real = super.createComponentPopupBuilder(content, preferableFocusComponent);
        return (ComponentPopupBuilder) Proxy.newProxyInstance(ComponentPopupBuilder.class.getClassLoader(), new Class<?>[]{ComponentPopupBuilder.class}, (proxy, method, args) -> {
            final Object result;
            try {
                result = method.invoke(real, args);
            } catch (final InvocationTargetException ex) {
                throw ex.getCause();
            }
            if (result == real) return proxy;
            if (result instanceof final JBPopup popup) popup.setSize(content.getPreferredSize());
            return result;
        });
    }
}
