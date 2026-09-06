# Design

One document per screen: what it looks like, and the reasoning behind every
choice on it.

| | |
|---|---|
| **Answers** | Why this surface is shaped the way it is, and what every part of it does |
| **For** | Anyone changing a screen, and anyone deciding whether a change is allowed |
| **Owns** | No identifiers. A design document cites `BR` and `UC`; it does not create them |
| **Written to** | [How a document is written](../standard.md) — a numbered sketch per screen |

---

## Documents

| Document | The surface | State |
|---|---|---|
| **[Light mode](light-mode.md)** | The standalone always-on-top window that shows one test case at a time | **Written** — [#13](https://github.com/mtb550/test-in/issues/13), closed |
| Test case editor | The grid, the card list, the details panel and the toolbar that drives them | Not written |
| The dialog framework | `ui.framework` — the dialog shell, the status bar strip, the keycap, the prose paragraph, the row striping. What a new dialog gets for free | Not written — [#11](https://github.com/mtb550/test-in/issues/11), [#69](https://github.com/mtb550/test-in/issues/69) |
| The project tree | The explorer: what each node offers, what may be dropped where, and the markers on disk behind it | Not written |
| Reports | What a generated report contains, and why each figure is on it | Not written |

---

## What a design document here is

**Written before the code, then checked against the build afterward.** The second
half is the one that gets skipped, and it is the one that matters: a page that
quietly stops matching the plugin is worse than no page, because it is believed.

**It records where it disagrees with the build.** Every document ends with a table
of what was drawn against what was built, and why the difference stands. A design
that describes an intention nobody kept is a description of nothing.

**It says which commit it was checked against**, so a reader can tell how far it
might have drifted without having to guess.

**It does not describe the code.** Class names appear only where naming one is the
shortest way to say which thing owns a decision. A design document should survive
a refactor of what it describes.

---

## Where a design document sits

A screen is designed here, but the promise it keeps belongs upstream:

```
business-requirements/   what the product promises        BR-nn, UC-nn, Q-nn
        │
        ▼
system-requirements/     what the software must do        SR-nn
        │
        ▼
design/                  what the tester sees, and why    cites the above
        │
        ▼
the code                 the javadoc says how
```

So a design document may cite a rule and must not invent one. If a screen needs a
promise nothing has made, that promise is a change to the
[business requirements](../business-requirements/business-requirements.md), and
the screen waits for it.
