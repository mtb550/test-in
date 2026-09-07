[Documentation](../README.md) › [The view panel](main.md) › UC-VIEW-PANEL-002

# UC-VIEW-PANEL-002: Let the panel follow the selection

**As a** tester, **I want** the panel to keep up as I move down the test cases,
**so that** I can read each one without asking for it every time.

There is no key for this. It happens once the panel is open.

## Rules

- **Rule-VIEW-PANEL-015** — Moving the selection in an editor fills the panel
  again, but only while the panel is on screen.
- **Rule-VIEW-PANEL-016** — Following never opens the panel. A panel the tester
  closed does not come back on the next click.
- **Rule-VIEW-PANEL-017** — Moving to a Testin editor with nothing selected
  empties the panel.
- **Rule-VIEW-PANEL-018** — Every fill starts again at the first test case, so
  the paging position is never carried over.

Rule-VIEW-PANEL-001 to Rule-VIEW-PANEL-009 hold everywhere in the panel. They
are on [the view panel page](main.md#rules-that-hold-everywhere-in-the-panel).

## Main flow

1. The panel is open on a test case.
2. The tester presses the down arrow, or clicks another card.
3. The panel draws the newly selected test case.
4. The tester moves to another Testin editor tab.
5. The panel draws whatever that editor has selected.

## What Testin refuses

**If the panel is closed** — nothing happens. Moving the selection does not open
it. A tester who closed the panel asked for the screen, and gets to keep it.

**If the new selection is empty** — the panel keeps the test case it was
showing.

**If the tester moves to a Testin editor with nothing selected** — the panel is
emptied, and reads *Select a test case to view details*.

**If the tester closes any Testin editor** — the panel is emptied, whichever
editor closed and whatever the panel was showing. That is difference 9 on
[the view panel page](main.md#where-the-plugin-breaks-its-own-rules).

---

[Documentation](../README.md) › [The view panel](main.md)
