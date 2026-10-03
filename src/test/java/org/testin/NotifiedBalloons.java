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

package org.testin;

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotifiedBalloons {

    public static @NotNull List<String> watched(final @NotNull Project p, final @NotNull Disposable owner) {
        final @NotNull List<String> said = new CopyOnWriteArrayList<>();
        Services.getInstance(p, Notifier.class).watchBalloons(owner, html -> said.add(StringUtil.unescapeXmlEntities(StringUtil.removeHtmlTags(html.replace("<br>", "\n"))).trim()));
        return said;
    }

    public static @NotNull List<Notification> notifications(final @NotNull Project p, final @NotNull Disposable owner) {
        final @NotNull List<Notification> said = new CopyOnWriteArrayList<>();
        p.getMessageBus().connect(owner).subscribe(Notifications.TOPIC, new Notifications() {
            @Override
            public void notify(final @NotNull Notification notification) {
                said.add(notification);
            }
        });
        return said;
    }
}
