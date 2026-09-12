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

package org.testin.view;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * This project's view panel, from the moment its tool window builds it.
 * <p>
 * One panel per project, because there is one tool window per project. It was
 * a static field, which is one slot for the whole IDE: with two projects open
 * the second to open the view overwrote the first, and every reader after that
 * - the selection listener, the editors, the update aftermath - reached the
 * wrong project's panel. Selecting a case in project A showed it in project B's
 * tool window, and closing B left A's panel unreachable.
 * <p>
 * The static also outlived the project it belonged to, which {@code dispose}
 * had to clear by hand to stop a closed project being held alive. A project
 * service is disposed with its project, so that is no longer anybody's job.
 */
@Service(Service.Level.PROJECT)
public final class ViewPanelHolder {

    private @NotNull Optional<ViewPanel> panel = Optional.empty();

    /**
     * The panel, empty until this project's tool window has built one.
     */
    @NotNull Optional<ViewPanel> get() {
        return panel;
    }

    void hold(final @NotNull ViewPanel built) {
        panel = Optional.of(built);
    }

    /**
     * Forgets the panel, if the one closing is still the one held.
     * <p>
     * <b>Which today means at project close and nowhere else.</b> A panel
     * registers itself on the project, so the project is what disposes it, and
     * by then nothing is going to ask for it again. The check is cheap
     * insurance rather than a live case: it is here so that a panel disposed
     * while a newer one is held cannot throw the newer one away.
     * <p>
     * <b>Why the project and not the tool window's Content.</b> Registering on
     * the Content would tie the panel to what builds it, which is the shape that
     * would make this method matter - but the view tool window has three
     * Contents, one per tab, and they all draw scroll panes of the same panel.
     * There is no single Content to be its owner, and picking one of three is a
     * lifetime decided by which tab happens to close first. So the project owns
     * it, the way the tree panel is owned by the project rather than by the
     * content that shows it (#66, finding 78).
     * <p>
     * The cost is a panel that outlives its tool window if the platform ever
     * builds the content twice: the old one keeps its font subscriptions and its
     * execution subscriber, refreshing tabs nobody can see. Nothing in the
     * plugin causes that today, and the fix for it is a single owner for the
     * three Contents rather than a second Disposer parent here.
     */
    void release(final @NotNull ViewPanel closing) {
        if (panel.filter(held -> held == closing).isPresent()) panel = Optional.empty();
    }
}
