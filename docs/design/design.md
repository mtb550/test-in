[Documentation](../README.md) › Design

# Design

One document per screen. Each says what the screen looks like, and why every
choice on it was made.

| | |
|---|---|
| **Answers** | Why this screen is shaped the way it is, and what every part of it does |
| **For** | Anyone changing a screen, and anyone deciding whether a change is allowed |
| **Owns** | Nothing of its own. A design document points at the rules and use cases. It never invents one |
| **Written to** | [How a document is written](../standard.md). One numbered sketch per screen |

---

## Documents

| Document | The screens it draws | Where it stands |
|---|---|---|
| **[The project panel](project-panel.md)** | The tree, its menu and every dialog it opens. 13 screens | **Written** — [#181](https://github.com/mtb550/test-in/issues/181) |
| The test case editor | The grid, the card list, the details panel and the toolbar | Not written — [#181](https://github.com/mtb550/test-in/issues/181) |
| The test run editor | Execution, verdicts and the failure form | Not written — [#181](https://github.com/mtb550/test-in/issues/181) |
| **[Light mode](light-mode.md)** | The always-on-top window that shows one test case at a time. Part of the test run editor | **Written** — [#13](https://github.com/mtb550/test-in/issues/13), closed |
| The view panel | Details, history and bugs | Not written — [#181](https://github.com/mtb550/test-in/issues/181) |
| The settings page | The settings page | Not written — [#181](https://github.com/mtb550/test-in/issues/181) |
| Reports, export, import and sync | The report, export, import and sync dialogs | Not written — [#181](https://github.com/mtb550/test-in/issues/181) |
| The dialog framework | What every dialog gets for free: the shell, the status bar, the key caps, the row striping | Not written — [#11](https://github.com/mtb550/test-in/issues/11), [#69](https://github.com/mtb550/test-in/issues/69) |

---

## What a design document here is

**It is written before the code, then checked against the build.** The second
half is the one that gets skipped, and it is the one that matters. A page that
quietly stops matching the plugin is worse than no page, because it is believed.

**It records where it disagrees with the build.** Every document ends with a
table of what was drawn against what was built. It says why the difference
stands. A design that describes an intention nobody kept describes nothing.

**It says which commit it was checked against.** So a reader can tell how far it
might have drifted, instead of guessing.

**It does not describe the code.** A class name appears only where naming it is
the shortest way to say which thing owns a decision. A design document should
survive a refactor of what it describes.

---

## Where a design document sits

A screen is designed here. The promise it keeps is written one level up.

The [business requirements](../business-requirements/business-requirements.md)
say what the product promises. The
[system requirements](../system-requirements/system-requirements.md) say what
the software must do to keep that promise. These design documents say what the
tester sees, and why. The code says how, in its own comments.

So a design document may point at a rule. It must never invent one. If a screen
needs a promise nobody has made, that promise is a change to the
[business requirements](../business-requirements/business-requirements.md). The
screen waits for it.

---

[Documentation](../README.md) › **Design** — the other two: [business requirements](../business-requirements/business-requirements.md) · [system requirements](../system-requirements/system-requirements.md)
